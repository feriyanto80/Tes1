package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tencentcs.iotvideo.IoTVideoSdk;
import com.tencentcs.iotvideo.iotvideoplayer.CallTypeEnum;
import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener;
import com.tencentcs.iotvideo.iotvideoplayer.IPreparedListener;
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener;
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoView;
import com.tencentcs.iotvideo.iotvideoplayer.PlayerStateEnum;
import com.tencentcs.iotvideo.iotvideoplayer.player.LivePlayer;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native IoTVideo P2P diagnostics. Never exports account credentials or SDK raw logs. */
public final class NativeP2PActivity extends Activity {
    private final int bg=Color.rgb(10,18,31),surface=Color.rgb(25,37,57),
            accent=Color.rgb(32,196,162),white=Color.WHITE,muted=Color.rgb(178,189,202);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final StringBuilder eventLog=new StringBuilder();
    private TextView status;
    private IoTVideoView videoView;
    private LivePlayer player;
    private String deviceId="";
    private String lastState="NOT_STARTED";
    private volatile int errorCode=-1;
    private volatile boolean prepared=false;
    private volatile boolean registerCallReturned=false;
    private volatile boolean playSeen=false;
    private Runnable watchdog;
    private long sessionStartElapsed=0L;
    private long registerReturnedElapsed=0L;
    private long preparedElapsed=0L;
    private long playElapsed=0L;
    private String networkAtStart="UNKNOWN";
    private final ExecutorService authorizationExecutor=Executors.newSingleThreadExecutor();
    private volatile String preflightState="NOT_TESTED";
    private volatile String preflightDetail="";
    private int preflightGeneration=0;
    private volatile boolean mediaVerified=false;
    private Runnable mediaObserver;
    private String selectedDefinition="HD";

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String txt,int sp,int color,boolean bold){
        TextView t=new TextView(this);t.setText(txt);t.setTextSize(sp);t.setTextColor(color);
        t.setPadding(dp(4),dp(7),dp(4),dp(7));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private GradientDrawable shape(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;
    }
    private Button button(String title,int color){
        Button b=new Button(this);b.setText(title);b.setTextColor(white);b.setAllCaps(false);
        b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(color));return b;
    }
    private void event(String value){
        synchronized(eventLog){
            if(eventLog.length()>6500)eventLog.delete(0,eventLog.length()-4500);
            eventLog.append(System.currentTimeMillis()).append(" | ").append(value).append("\n");
        }
        lastState=value;
        runOnUiThread(()->{if(!isFinishing()&&status!=null)status.setText(value);});
    }
    private String stateName(int s){
        if(s==PlayerStateEnum.STATE_INITIALIZED)return "INITIALIZED";
        if(s==PlayerStateEnum.STATE_PREPARING)return "PREPARING";
        if(s==PlayerStateEnum.STATE_READY)return "READY";
        if(s==PlayerStateEnum.STATE_LOADING)return "LOADING";
        if(s==PlayerStateEnum.STATE_PLAY)return "PLAY";
        if(s==PlayerStateEnum.STATE_PAUSE)return "PAUSE";
        if(s==PlayerStateEnum.STATE_STOP)return "STOP";
        if(s==PlayerStateEnum.STATE_IDLE)return "IDLE";
        return "UNKNOWN("+s+")";
    }
    private String maskedDeviceId(){
        if(deviceId==null||deviceId.length()<6)return "[REDACTED]";
        return deviceId.substring(0,3)+"…"+deviceId.substring(deviceId.length()-2);
    }
    private String networkSnapshot(){
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            Network n=cm.getActiveNetwork();
            if(n==null)return "NO_ACTIVE_NETWORK";
            NetworkCapabilities c=cm.getNetworkCapabilities(n);
            if(c==null)return "CAPABILITIES_UNAVAILABLE";
            String transport=c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"WIFI":
                    c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"CELLULAR":
                    c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"ETHERNET":
                    c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)?"VPN":"OTHER";
            return transport+"; internet="+c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)+
                    "; validated="+c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }catch(Throwable t){return "NETWORK_DIAGNOSTIC_ERROR:"+t.getClass().getSimpleName();}
    }
    private String elapsed(long t){return t<=0?"NOT_REACHED":(t-sessionStartElapsed)+"ms";}
    private String playerMetrics(){
        if(player==null)return "player=null";
        try{
            return "sdkPlayState="+stateName(player.getPlayState())+
                    "; isPlaying="+player.isPlaying()+
                    "; video="+player.getVideoWidth()+"x"+player.getVideoHeight()+
                    "; avBytesPerSec="+player.getAvBytesPerSec();
        }catch(Throwable t){
            return "metrics_error="+t.getClass().getSimpleName();
        }
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        deviceId=getIntent().getStringExtra("device_id");
        if(deviceId==null)deviceId="";
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(bg);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14),dp(20),dp(14),dp(30));sc.addView(body);setContentView(sc);
        body.addView(label("PBS · GWELL NATIVE P2P",14,accent,true));
        body.addView(label("Live Video via Device ID",23,white,true));
        body.addView(label("Device ID: "+deviceId,14,muted,false));
        body.addView(label("SDK: GWTimes IoTVideo. Inisialisasi aplikasi dan SDK logging aktif. Tidak memakai IP/RTSP.",12,muted,false));
        LinearLayout frame=new LinearLayout(this);frame.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,dp(270));vp.topMargin=dp(12);body.addView(frame,vp);
        videoView=(IoTVideoView)getLayoutInflater().inflate(R.layout.pbs_native_p2p_view,frame,false);
        frame.addView(videoView,new LinearLayout.LayoutParams(-1,-1));
        status=label("SDK: "+PbsGwellApplication.getInitStatus()+"\nBelum memulai sesi P2P.",13,white,false);
        status.setTextIsSelectable(true);body.addView(status);
        Button preflight=button("🔎 Cek relasi akun–kamera (read-only)",surface);
        Button play=button("▶ Diagnostik Live P2P HD",Color.rgb(27,142,120));
        Button playSd=button("▶ Diagnostik Live P2P SD",Color.rgb(27,142,120));
        Button stop=button("■ Hentikan P2P",surface);
        Button share=button("Salin Hasil Uji (Tanpa Token)",surface);
        for(Button x:new Button[]{preflight,play,playSd,stop,share}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(47));lp.topMargin=dp(9);body.addView(x,lp);
        }
        preflight.setOnClickListener(v->runAuthorizationPreflight(false));
        play.setOnClickListener(v->{selectedDefinition="HD";runAuthorizationPreflight(true);});
        playSd.setOnClickListener(v->{selectedDefinition="SD";runAuthorizationPreflight(true);});
        stop.setOnClickListener(v->stopP2P());
        share.setOnClickListener(v->copyReport());
        body.addView(label("v1.2.7 · Preflight relasi perangkat menggunakan Cloud API read-only; daftar dapat dipaginasi. Kode 8003 pada GWTimes ASrv berarti relasi device–user tidak sah, tetapi kesesuaian mapping Android harus dikonfirmasi. Tidak melakukan bind/unbind otomatis dan tidak mengubah izin kamera.",12,muted,false));
        if(!P2PSession.ready())event("SESSION_MISSING: Login cloud dahulu, lalu pilih Device ID kembali.");
    }
    private void runAuthorizationPreflight(boolean launchAfter) {
        if(deviceId.isEmpty()||!deviceId.matches("[A-Za-z0-9_-]{1,100}")){
            event("DEVICE_ID_INVALID");return;
        }
        if(!P2PSession.ready()){
            event("SESSION_MISSING: login cloud diperlukan");return;
        }
        final int generation=++preflightGeneration;
        if(launchAfter)stopPlayerQuietly();
        synchronized(eventLog){eventLog.setLength(0);}
        preflightState="RUNNING";preflightDetail="";
        event("AUTH_PREFLIGHT_START: authenticated read-only device list and device info; no binding changes");
        authorizationExecutor.execute(()->{
            final P2PAuthorizationProbe.Result outcome=P2PAuthorizationProbe.check(deviceId);
            runOnUiThread(()->{
                if(isFinishing()||generation!=preflightGeneration)return;
                preflightState=outcome.state;
                preflightDetail=outcome.detail;
                event("AUTH_PREFLIGHT_RESULT: "+outcome.state+"; "+outcome.detail);
                if(launchAfter){
                    event("AUTH_NOTE: preflight does NOT prove streaming entitlement; server-side relation is authoritative");
                    startP2P();
                }
            });
        });
    }
    private void startP2P(){
        if(deviceId.isEmpty()||!deviceId.matches("[A-Za-z0-9_-]{1,100}")){
            event("DEVICE_ID_INVALID");return;
        }
        if(!P2PSession.ready()){
            event("SESSION_MISSING: Login Gwell Cloud diperlukan.");return;
        }
        if(!PbsGwellApplication.getInitStatus().startsWith("SDK_INIT_OK")){
            event("SDK_INIT_FAILED: "+PbsGwellApplication.getInitStatus());return;
        }
        try{
            stopPlayerQuietly();
            errorCode=-1;prepared=false;registerCallReturned=false;playSeen=false;
            sessionStartElapsed=SystemClock.elapsedRealtime();registerReturnedElapsed=0L;preparedElapsed=0L;playElapsed=0L;
            networkAtStart=networkSnapshot();mediaVerified=false;
            event("START: SDK initialized at Application startup; "+PbsGwellApplication.getInitStatus()+"; network="+networkAtStart);
            long accessId=Long.parseLong(P2PSession.id());
            IoTVideoSdk.register(accessId,P2PSession.token());
            registerCallReturned=true;registerReturnedElapsed=SystemClock.elapsedRealtime();
            event("REGISTER_CALL_RETURNED: register() is void; this does NOT prove server authentication; elapsed="+elapsed(registerReturnedElapsed));

            player=new LivePlayer();
            player.setDataResource(deviceId,
                    "SD".equals(selectedDefinition)?CallTypeEnum.VIDEO_DEFINITION_SD:CallTypeEnum.VIDEO_DEFINITION_HD,(short)0);
            player.setPreparedListener(new IPreparedListener(){
                @Override public void onPrepared(){
                    prepared=true;preparedElapsed=SystemClock.elapsedRealtime();event("PREPARED: SDK reports player prepared; waiting for stream/frame; elapsed="+elapsed(preparedElapsed));
                }
            });
            player.setStatusListener(new IStatusListener(){
                @Override public void onStatus(int state){
                    String name=stateName(state);
                    if(state==PlayerStateEnum.STATE_PLAY){playSeen=true;playElapsed=SystemClock.elapsedRealtime();}
                    event("STATUS: "+name+" ("+state+")");
                }
            });
            player.setErrorListener(new IErrorListener(){
                @Override public void onError(int error){
                    errorCode=error;event("SDK_ERROR: code="+error+"; "+playerMetrics());
                    if(error==8003){
                        event("ERROR_8003_HINT: GWTimes IoTVideo ASrv maps 8003 to INVALID_DEVICE_USER_RELATIONSHIP. Android server-origin mapping unverified. Check PBS account binding/share, customer/product namespace, and IoTVideo backend; this is NOT a decoder diagnosis.");
                    }
                }
            });
            player.setVideoView(videoView);
            event("PLAY_REQUEST: Device ID="+maskedDeviceId()+"; definition="+selectedDefinition+"; sourceId=0; preflight="+preflightState+"; network="+networkSnapshot());
            player.play();
            mediaObserver=new Runnable(){
                @Override public void run(){
                    if(player==null || mediaVerified)return;
                    try{
                        int width=player.getVideoWidth(),height=player.getVideoHeight();
                        int bps=player.getAvBytesPerSec();
                        if(playSeen && width>0 && height>0 && bps>0){
                            mediaVerified=true;
                            event("MEDIA_VERIFIED: STATE_PLAY + dimensions="+width+"x"+height+
                                " + avBytesPerSec="+bps+"; elapsed="+elapsed(SystemClock.elapsedRealtime()));
                            return;
                        }
                    }catch(Throwable t){event("MEDIA_OBSERVER_ERROR: "+t.getClass().getSimpleName());return;}
                    handler.postDelayed(this,2000);
                }
            };
            handler.postDelayed(mediaObserver,2000);
            watchdog=()->{
                if(player!=null&&!mediaVerified){
                    event("WATCHDOG_25S: no confirmed live frames; STATE_PLAY="+playSeen+"; auth=NOT_CONFIRMED_BY_VOID_REGISTER; preflight="+preflightState+"; prepared="+prepared+
                            "; registerCallReturned="+registerCallReturned+"; registerElapsed="+elapsed(registerReturnedElapsed)+
                            "; preparedElapsed="+elapsed(preparedElapsed)+"; network="+networkSnapshot()+"; "+playerMetrics());
                }
            };
            handler.postDelayed(watchdog,25000);
        }catch(Throwable t){
            event("P2P_SETUP_ERROR: "+t.getClass().getSimpleName()+" — "+safeMessage(t));
        }
    }
    private String safeMessage(Throwable t){
        // Third-party exception messages can contain IDs, URLs, or secrets.
        return "(vendor error message redacted)";
    }
    private void stopPlayerQuietly(){
        if(mediaObserver!=null){handler.removeCallbacks(mediaObserver);mediaObserver=null;}
        if(watchdog!=null){handler.removeCallbacks(watchdog);watchdog=null;}
        if(player!=null){
            try{player.stop();}catch(Throwable ignored){}
            try{player.release();}catch(Throwable ignored){}
            player=null;
        }
    }
    private void stopP2P(){
        ++preflightGeneration;
        stopPlayerQuietly();
        event("USER_STOP: pemutaran dihentikan oleh pengguna.");
    }
    private void copyReport(){
        String events;
        synchronized(eventLog){events=eventLog.toString();}
        String report="PBS Native P2P diagnostic · build 1.2.7"+
                "\nDevice ID: "+maskedDeviceId()+
                "\nSDK init: "+PbsGwellApplication.getInitStatus()+
                "\nprepared: "+prepared+
                "\nregisterCallReturned: "+registerCallReturned+
                "\nplaySeen: "+playSeen+
                "\nmediaVerified: "+mediaVerified+
                "\npreflightState: "+preflightState+
                "\npreflightDetail: "+preflightDetail+
                "\nerrorCode: "+errorCode+
                "\nnetworkAtStart: "+networkAtStart+
                "\nregisterElapsed: "+elapsed(registerReturnedElapsed)+
                "\npreparedElapsed: "+elapsed(preparedElapsed)+
                "\nplayElapsed: "+elapsed(playElapsed)+
                "\nmetrics: "+playerMetrics()+
                "\nEVENTS:\n"+events+
                "\nCredentials and raw SDK logs intentionally excluded.";
        ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("PBS Gwell P2P diagnostics",report));
        event("DIAGNOSTIC_COPIED: laporan aman disalin ke clipboard.");
    }
    @Override protected void onResume(){
        super.onResume();
        if(videoView!=null)try{videoView.onResume();}catch(Throwable ignored){}
    }
    @Override protected void onPause(){
        if(videoView!=null)try{videoView.onPause();}catch(Throwable ignored){}
        super.onPause();
    }
    @Override protected void onStop(){
        ++preflightGeneration;
        stopPlayerQuietly();
        super.onStop();
    }
    @Override protected void onDestroy(){
        stopPlayerQuietly();
        authorizationExecutor.shutdownNow();
        super.onDestroy();
    }
}
