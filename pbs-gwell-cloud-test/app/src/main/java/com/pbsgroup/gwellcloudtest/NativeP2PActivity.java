package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;

import com.tencentcs.iotvideo.IoTVideoSdk;
import com.tencentcs.iotvideo.iotvideoplayer.CallTypeEnum;
import com.tencentcs.iotvideo.iotvideoplayer.IErrorListener;
import com.tencentcs.iotvideo.iotvideoplayer.IPreparedListener;
import com.tencentcs.iotvideo.iotvideoplayer.IStatusListener;
import com.tencentcs.iotvideo.iotvideoplayer.IoTVideoView;
import com.tencentcs.iotvideo.iotvideoplayer.PlayerStateEnum;
import com.tencentcs.iotvideo.iotvideoplayer.player.LivePlayer;

/** Genuine native Gwell/Tencent IoTVideo SDK playback experiment (not RTSP). */
public final class NativeP2PActivity extends Activity {
    private final int bg=Color.rgb(10,18,31),surface=Color.rgb(25,37,57),
        accent=Color.rgb(32,196,162),white=Color.WHITE,muted=Color.rgb(178,189,202);
    private TextView status;
    private IoTVideoView videoView;
    private LivePlayer player;
    private boolean sdkInitialized=false;
    private boolean registered=false;
    private String deviceId="";
    private String lastState="NOT_STARTED";
    private int errorCode=-1;

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String txt,int sp,int color,boolean bold){
        TextView t=new TextView(this);t.setText(txt);t.setTextSize(sp);t.setTextColor(color);
        t.setPadding(dp(4),dp(7),dp(4),dp(7));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable shape(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;
    }
    private Button button(String title,int color){
        Button b=new Button(this);b.setText(title);b.setTextColor(white);b.setAllCaps(false);
        b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(color));return b;
    }
    private void show(String text){
        lastState=text;
        runOnUiThread(()->{if(!isFinishing()&&status!=null)status.setText(text);});
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        deviceId=getIntent().getStringExtra("device_id");
        if(deviceId==null)deviceId="";
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(bg);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14),dp(20),dp(14),dp(30));
        sc.addView(body);
        setContentView(sc);
        body.addView(label("PBS · GWELL NATIVE P2P",14,accent,true));
        body.addView(label("Live Video via Device ID",23,white,true));
        body.addView(label("Device ID: "+deviceId,14,muted,false));
        body.addView(label("SDK: GWTimes IoTVideo (arsip 2020). P2P langsung melalui SDK; tidak menggunakan IP, RTSP, ataupun ONVIF.",12,muted,false));
        LinearLayout frame=new LinearLayout(this);frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,dp(270));vp.topMargin=dp(12);
        body.addView(frame,vp);
        // Video is rendered through the SDK's proprietary IoTVideoView.
        videoView=(IoTVideoView)getLayoutInflater().inflate(R.layout.pbs_native_p2p_view,frame,false);
        frame.addView(videoView,new LinearLayout.LayoutParams(-1,-1));
        status=label("Belum diuji. Login cloud pada halaman sebelumnya, lalu tekan Mulai P2P.",13,white,false);
        status.setTextIsSelectable(true);body.addView(status);
        Button play=button("▶ Mulai Live P2P",Color.rgb(27,142,120));
        Button stop=button("■ Hentikan P2P",surface);
        Button share=button("Salin Hasil Uji (Tanpa Token)",surface);
        for(Button x:new Button[]{play,stop,share}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(47));lp.topMargin=dp(9);body.addView(x,lp);
        }
        play.setOnClickListener(v->startP2P());
        stop.setOnClickListener(v->stopP2P());
        share.setOnClickListener(v->{
            String report="PBS Native P2P v1.2.0\nDevice ID: "+deviceId+
                "\nState: "+lastState+"\nError SDK: "+errorCode+
                "\nNo access token or password included.";
            ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("PBS Gwell P2P diagnostics",report));
            show("Laporan tanpa token disalin.\n"+lastState);
        });
        body.addView(label("Bukti sukses P2P hanya jika player mencapai status STATE_PLAY dan video terlihat. Jika SDK menolak token, hentikan pengujian dan gunakan kredensial developer resmi. Tidak ada PTZ native pada demo IoTVideo ini.",12,muted,false));
        if(!P2PSession.ready()){
            show("SESSION_MISSING: Tidak ada sesi cloud di memori. Kembali, login ulang, ambil daftar perangkat, lalu buka kembali Native P2P.");
        }
    }
    private void startP2P(){
        if(deviceId.isEmpty()||!deviceId.matches("[A-Za-z0-9_-]{1,100}")){
            show("DEVICE_ID_INVALID: ID kamera belum ada.");return;
        }
        if(!P2PSession.ready()){
            show("SESSION_MISSING: Login Gwell Cloud diperlukan pada aplikasi ini.");return;
        }
        try{
            stopPlayerQuietly();
            show("SDK_INIT: Menginisialisasi SDK video. Belum membuktikan kamera tersambung.");
            if(!sdkInitialized){
                IoTVideoSdk.init(getApplicationContext(),null);
                sdkInitialized=true;
            }
            if(!registered){
                long id=Long.parseLong(P2PSession.id());
                IoTVideoSdk.register(id,P2PSession.token());
                registered=true;
                show("REGISTER_CALLED: SDK register() dipanggil. Keberhasilan autentikasi jaringan belum terkonfirmasi.");
            }
            player=new LivePlayer();
            player.setDataResource(deviceId,CallTypeEnum.VIDEO_DEFINITION_HD,(short)0);
            player.setVideoView(videoView);
            player.setPreparedListener(new IPreparedListener(){
                @Override public void onPrepared(){
                    show("P2P_PREPARED: Perangkat/pemutar siap. Menunggu frame dan status play.");
                }
            });
            player.setStatusListener(new IStatusListener(){
                @Override public void onStatus(int state){
                    if(state==PlayerStateEnum.STATE_PLAY)
                        show("P2P_STATE_PLAY: SDK melaporkan sedang memutar. Pastikan gambar terlihat.");
                    else if(state==PlayerStateEnum.STATE_STOP)
                        show("P2P_STATE_STOP: Stream berhenti.");
                    else if(state==PlayerStateEnum.STATE_PAUSE)
                        show("P2P_STATE_PAUSE: Stream dijeda.");
                    else show("P2P_STATE_"+state+": Menunggu sesi/video SDK.");
                }
            });
            player.setErrorListener(new IErrorListener(){
                @Override public void onError(int error){
                    errorCode=error;
                    show("P2P_ERROR "+error+": SDK menolak/memutus stream. Periksa kompatibilitas SDK, token, firmware dan hak perangkat.");
                }
            });
            show("P2P_CONNECTING: Mencoba LivePlayer.play() ke Device ID "+deviceId+".");
            player.play();
        }catch(Throwable t){
            show("P2P_SETUP_ERROR: "+t.getClass().getSimpleName()+
                " — "+safeMessage(t)+". SDK tidak berhasil menyiapkan P2P.");
        }
    }
    private String safeMessage(Throwable t){
        String m=t.getMessage();
        if(m==null)return "(no detail)";
        // Avoid leaking URLs/possible access tokens in low-level error messages.
        if(m.length()>140)m=m.substring(0,140);
        if(m.contains("accessToken")||m.contains("token="))return "(detail berpotensi memuat token)";
        return m;
    }
    private void stopPlayerQuietly(){
        if(player==null)return;
        try{player.stop();}catch(Throwable ignored){}
        try{player.release();}catch(Throwable ignored){}
        player=null;
    }
    private void stopP2P(){
        stopPlayerQuietly();
        show("P2P_STOPPED: Pemutaran dihentikan.");
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
