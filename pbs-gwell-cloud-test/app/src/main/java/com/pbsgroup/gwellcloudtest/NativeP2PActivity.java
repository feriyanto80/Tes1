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
    private int errorCode=-1;
    private boolean prepared=false;
    private boolean registerCallReturned=false;
    private boolean playSeen=false;
    private Runnable watchdog;
    private long sessionStartElapsed=0L;
    private long registerReturnedElapsed=0L;
    private long preparedElapsed=0L;
    private long playElapsed=0L;
    private String networkAtStart="UNKNOWN";

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
        Button play=button("▶ Mulai Live P2P",Color.rgb(27,142,120));
        Button stop=button("■ Hentikan P2P",surface);
        Button share=button("Salin Hasil Uji (Tanpa Token)",surface);
        for(Button x:new Button[]{play,stop,share}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(47));lp.topMargin=dp(9);body.addView(x,lp);
        }
        play.setOnClickListener(v->startP2P());
        stop.setOnClickListener(v->stopP2P());
        share.setOnClickListener(v->copyReport());
        body.addView(label("Versi ini merekam nama state, callback error, status prepared, dimensi video, laju data dan status player. Laporan tidak menyertakan password, accessToken, AppToken atau raw SDK log.",12,muted,false));
        if(!P2PSession.ready())event("SESSION_MISSING: Login cloud dahulu, lalu pilih Device ID kembali.");
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
            synchronized(eventLog){eventLog.setLength(0);}
            errorCode=-1;prepared=false;registerCallReturned=false;playSeen=false;
            sessionStartElapsed=SystemClock.elapsedRealtime();registerReturnedElapsed=0L;preparedElapsed=0L;playElapsed=0L;
            networkAtStart=networkSnapshot();
            event("START: SDK initialized at Application startup; "+PbsGwellApplication.getInitStatus()+"; network="+networkAtStart);
            long accessId=Long.parseLong(P2PSession.id());
            IoTVideoSdk.register(accessId,P2PSession.token());
            registerCallReturned=true;registerReturnedElapsed=SystemClock.elapsedRealtime();
            event("REGISTER_CALL_RETURNED: register() is void; this does NOT prove server authentication; elapsed="+elapsed(registerReturnedElapsed));

            player=new LivePlayer();
            player.setDataResource(deviceId,CallTypeEnum.VIDEO_DEFINITION_HD,(short)0);
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
                }
            });
            player.setVideoView(videoView);
            event("PLAY_REQUEST: Device ID="+maskedDeviceId()+"; definition=HD; sourceId=0; network="+networkSnapshot());
            player.play();
            watchdog=()->{
                if(player!=null&&!playSeen){
                    event("WATCHDOG_25S: no STATE_PLAY; auth=NOT_CONFIRMED_BY_VOID_REGISTER; prepared="+prepared+
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
        String m=t.getMessage();
        if(m==null)return "(no detail)";
        if(m.length()>140)m=m.substring(0,140);
        if(m.toLowerCase(Locale.ROOT).contains("token")||m.contains("accessId"))return "(detail disamarkan untuk keamanan)";
        return m;
    }
    private void stopPlayerQuietly(){
        if(watchdog!=null){handler.removeCallbacks(watchdog);watchdog=null;}
        if(player!=null){
            try{player.stop();}catch(Throwable ignored){}
            try{player.release();}catch(Throwable ignored){}
            player=null;
        }
    }
    private void stopP2P(){
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
        stopPlayerQuietly();
        super.onStop();
    }
    @Override protected void onDestroy(){
        stopPlayerQuietly();
        super.onDestroy();
    }
}
