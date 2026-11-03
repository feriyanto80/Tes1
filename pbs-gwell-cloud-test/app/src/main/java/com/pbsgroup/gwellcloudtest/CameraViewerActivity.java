package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import java.util.List;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CameraViewerActivity extends Activity {
    private final int bg=Color.rgb(9,16,29),panel=Color.rgb(24,36,55),accent=Color.rgb(36,197,164);
    private final int white=Color.WHITE,secondary=Color.rgb(175,188,203);
    private final ExecutorService ptzJobs=Executors.newSingleThreadExecutor();
    private final ExecutorService discoveryJobs=Executors.newSingleThreadExecutor();
    private EditText hostInput,usernameInput,passwordInput,rtspPortInput,rtspPathInput,onvifPortInput;
    private TextView status;
    private PlayerView screen;
    private ExoPlayer player;
    private OnvifControl ptz;
    private LinearLayout controls,configuration;
    private boolean fullscreen=false;
    private String cameraId="";
    private float speed=0.35f;
    private int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+0.5f);}
    private GradientDrawable shape(int color){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(13));return g;
    }
    private TextView text(String content,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(content);t.setTextSize(size);t.setTextColor(color);
        t.setPadding(dp(4),dp(7),dp(4),dp(7));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private Button button(String label,boolean primary){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(primary?bg:white);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(primary?accent:panel));return b;
    }
    private void addBtn(LinearLayout root,String label,boolean primary,View.OnClickListener fn){
        Button b=button(label,primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(47));
        lp.topMargin=dp(8);root.addView(b,lp);b.setOnClickListener(fn);
    }
    private EditText field(LinearLayout root,String label,String value,boolean secret){
        root.addView(text(label,12,secondary,false));
        EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setTextSize(14);
        e.setTextColor(white);e.setHintTextColor(secondary);e.setPadding(dp(10),0,dp(10),0);
        e.setBackground(shape(panel));
        if(secret)e.setInputType(129);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.bottomMargin=dp(8);
        root.addView(e,p);return e;
    }
    private String extra(String key,String fallback){
        String s=getIntent().getStringExtra(key);return s==null?fallback:s;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        cameraId=extra("device_id","");
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);scroll.setFillViewport(true);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12),dp(14),dp(12),dp(30));scroll.addView(page);setContentView(scroll);
        TextView title=text("PBS CCTV · LIVE VIEW",20,white,true);
        page.addView(title);
        page.addView(text(cameraId.isEmpty()?"Kamera lokal":("Device ID: "+cameraId),12,secondary,false));
        screen=new PlayerView(this);screen.setUseController(true);screen.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams pv=new LinearLayout.LayoutParams(-1,dp(235));pv.topMargin=dp(10);
        page.addView(screen,pv);
        status=text("Device ID "+cameraId+" sudah terdaftar di cloud, tetapi server P2P video Gwell belum terhubung. Untuk RTSP gunakan IP kamera pada WiFi ONT yang sama atau VPN.",12,secondary,false);
        status.setTextIsSelectable(true);page.addView(status);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(actions);
        Button play=button("▶ Live RTSP",true);Button stop=button("■ Stop",false);
        Button full=button("⛶ Layar penuh",false);
        for(Button x:new Button[]{play,stop,full}){
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1f);
            p.setMargins(dp(2),dp(5),dp(2),dp(2));actions.addView(x,p);
        }
        play.setOnClickListener(v->startStream());
        stop.setOnClickListener(v->stopStream());
        full.setOnClickListener(v->toggleFullscreen());
        configuration=new LinearLayout(this);configuration.setOrientation(LinearLayout.VERTICAL);
        configuration.setPadding(dp(2),dp(10),dp(2),dp(2));page.addView(configuration);
        configuration.addView(text("KONEKSI KAMERA · ONT/LAN/VPN",13,accent,true));
        configuration.addView(text("Mode ini bukan P2P Cloud. Kamera yang berbeda ONT tanpa VPN tidak bisa ditemukan melalui multicast lokal.",12,secondary,false));
        addBtn(configuration,"🔎 Cari kamera ONVIF di WiFi ONT ini",true,v->discoverCameras());
        addBtn(configuration,"Buka Yoosee untuk Live P2P",false,v->openOfficialYoosee());
        hostInput=field(configuration,"IP/hostname kamera (LAN atau VPN)",extra("host",""),false);
        rtspPortInput=field(configuration,"RTSP Port",extra("rtsp_port","554"),false);
        rtspPathInput=field(configuration,"RTSP Path (bergantung firmware)",extra("rtsp_path","/onvif1"),false);
        onvifPortInput=field(configuration,"ONVIF Port",extra("onvif_port","5000"),false);
        usernameInput=field(configuration,"Username kamera",extra("username","admin"),false);
        passwordInput=field(configuration,"Password kamera (tidak disimpan)",extra("password",""),true);
        controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);page.addView(controls);
        controls.addView(text("PTZ CONTROL",15,accent,true));
        addBtn(controls,"Deteksi profil ONVIF + token PTZ",false,v->probeOnvif());
        addBtn(controls,"Ambil alamat RTSP otomatis dari ONVIF",true,v->autoRtsp());
        TextView hint=text("Tekan dan tahan arah untuk menggerakkan kamera; lepas untuk berhenti. Kontrol akan berfungsi jika ONVIF PTZ didukung kamera.",12,secondary,false);
        controls.addView(hint);
        LinearLayout speedBar=new LinearLayout(this);speedBar.setOrientation(LinearLayout.HORIZONTAL);controls.addView(speedBar);
        Button slow=button("Pelan",false),middle=button("Sedang",false),fast=button("Cepat",false);
        for(Button b0:new Button[]{slow,middle,fast}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),1f);
            lp.setMargins(dp(2),0,dp(2),0);speedBar.addView(b0,lp);
        }
        slow.setOnClickListener(v->{speed=.15f;status("Kecepatan PTZ: pelan");});
        middle.setOnClickListener(v->{speed=.35f;status("Kecepatan PTZ: sedang");});
        fast.setOnClickListener(v->{speed=.7f;status("Kecepatan PTZ: cepat");});
        for(int row=0;row<3;row++){
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
            controls.addView(line);
            for(int col=0;col<3;col++){
                int dx=col-1,dy=1-row;
                String glyph=dy==1?(dx==-1?"↖":dx==0?"↑":"↗"):
                    dy==0?(dx==-1?"←":dx==0?"■":"→"):
                    (dx==-1?"↙":dx==0?"↓":"↘");
                Button move=button(glyph,false);
                move.setTextSize(22);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(59),1f);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(move,lp);
                if(dx==0&&dy==0)move.setOnClickListener(v->sendStop());
                else {
                    final int x=dx,y=dy;
                    move.setOnTouchListener((v,event)->{
                        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                            sendMove(x,y);return true;
                        }
                        if(event.getActionMasked()==MotionEvent.ACTION_UP||
                            event.getActionMasked()==MotionEvent.ACTION_CANCEL){
                            sendStop();return true;
                        }
                        return true;
                    });
                }
            }
        }
        controls.addView(text("Mode P2P Cloud tidak tersedia di APK ini. Device ID dari Gwell tidak dapat langsung digunakan sebagai URL video. Untuk kamera pada ONT berbeda, aktifkan jalur privat/VPN atau SDK P2P yang disetujui vendor.",12,secondary,false));
    }
    private void discoverCameras(){
        status("Mencari ONVIF melalui multicast pada WiFi aktif (hingga 4 detik). Pastikan WiFi ini adalah ONT kamera.");
        discoveryJobs.execute(()->{
            try{
                final List<OnvifDiscovery.Camera> found=OnvifDiscovery.find(getApplicationContext());
                runOnUiThread(()->{
                    if(found.isEmpty()){
                        status("ONVIF discovery: 0 kamera. Pastikan HP pada WiFi ONT yang sama; AP isolation/non-ONVIF dapat memblokir deteksi. Tidak ada penemuan P2P melalui Device ID.");
                        return;
                    }
                    String[] values=new String[found.size()];
                    for(int i=0;i<found.size();i++)values[i]=found.get(i).label();
                    new AlertDialog.Builder(this)
                        .setTitle("Kamera ONVIF ditemukan: "+found.size())
                        .setMessage("Pilih IP kamera yang benar. Device ID cloud tidak otomatis mencocokkan kamera lokal.")
                        .setItems(values,(d,which)->{
                            OnvifDiscovery.Camera c=found.get(which);
                            hostInput.setText(c.address);
                            onvifPortInput.setText(String.valueOf(c.port));
                            ptz=null;
                            status("IP ditemukan dari WiFi: "+c.address+
                                ". Isi password kamera, tekan Live RTSP atau Deteksi profil ONVIF. Kamera belum dipetakan otomatis ke Device ID "+cameraId+".");
                        })
                        .setNegativeButton("Batal",null)
                        .show();
                });
            }catch(Exception e){status("ONVIF discovery gagal: "+e.getMessage());}
        });
    }
    private void openOfficialYoosee(){
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.yoosee");
        if(launch!=null){startActivity(launch);return;}
        try{
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.yoosee")));
        }catch(Exception e){
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.yoosee")));
        }
    }
    private String hostname(){
        String h=hostInput.getText().toString().trim();
        if(h.isEmpty())throw new IllegalArgumentException("IP belum diketahui. Klik 'Cari kamera ONVIF di WiFi ONT ini'. Device ID cloud bukan alamat IP RTSP.");
        if(!h.matches("[A-Za-z0-9.\\-]{1,200}"))throw new IllegalArgumentException("IP / hostname tidak valid");
        return h;
    }
    private int port(EditText e){
        int n=Integer.parseInt(e.getText().toString().trim());
        if(n<1||n>65535)throw new IllegalArgumentException("Port 1–65535");
        return n;
    }
    private String user(){return usernameInput.getText().toString();}
    private String pass(){return passwordInput.getText().toString();}
    private void status(String s){runOnUiThread(()->status.setText(s));}
    private void startStream(){
        try {
            String h=hostname(),u=user(),pw=pass();
            int p=port(rtspPortInput);
            String path=rtspPathInput.getText().toString().trim();
            if(path.isEmpty())path="/onvif1";
            if(!path.startsWith("/"))path="/"+path;
            if(path.contains(" ")||path.contains("#"))throw new IllegalArgumentException("RTSP path tidak valid");
            String auth=u.isEmpty()?"":Uri.encode(u)+":"+Uri.encode(pw)+"@";
            String stream="rtsp://"+auth+h+":"+p+path;
            stopStream();
            player=new ExoPlayer.Builder(this).build();screen.setPlayer(player);
            player.addListener(new Player.Listener(){
                @Override public void onPlaybackStateChanged(int state){
                    if(state==Player.STATE_BUFFERING)status("Menghubungkan RTSP ke IP kamera ...");
                    else if(state==Player.STATE_READY)status("LIVE RTSP BERHASIL · video dari IP kamera (bukan P2P Cloud)");
                    else if(state==Player.STATE_ENDED)status("Stream RTSP berakhir.");
                }
                @Override public void onPlayerError(PlaybackException e){
                    status("RTSP gagal: "+e.getErrorCodeName()+
                        ". Periksa IP, akses jaringan ONT/VPN, URL RTSP, password dan codec.");
                }
            });
            RtspMediaSource media=new RtspMediaSource.Factory().setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(stream));
            player.setMediaSource(media);player.prepare();player.play();
        }catch(Exception e){status("RTSP: "+e.getClass().getSimpleName()+": "+e.getMessage());}
    }
    private void stopStream(){
        if(player!=null){player.stop();player.release();player=null;}
        if(screen!=null)screen.setPlayer(null);
    }
    private synchronized OnvifControl control(){
        if(ptz==null)ptz=new OnvifControl(hostname(),port(onvifPortInput),user(),pass());
        return ptz;
    }
    private void autoRtsp(){
        try{
            final OnvifControl control=new OnvifControl(hostname(),port(onvifPortInput),user(),pass());
            status("Meminta ONVIF GetProfiles dan GetStreamUri ...");
            ptzJobs.execute(()->{
                try{
                    control.probe();
                    OnvifControl.StreamInfo stream=control.discoverRtsp();
                    runOnUiThread(()->{
                        rtspPortInput.setText(String.valueOf(stream.port));
                        rtspPathInput.setText(stream.path);
                        ptz=control;
                        status("RTSP URI ditemukan dari ONVIF. Port "+stream.port+
                            ", path "+stream.path+". Host tetap memakai IP kamera terpilih. Tekan Live RTSP.");
                    });
                }catch(Exception e){status("GetStreamUri gagal: "+e.getMessage());}
            });
        }catch(Exception e){status("ONVIF input belum lengkap: "+e.getMessage());}
    }
    private void probeOnvif(){
        try{
            final OnvifControl c=new OnvifControl(hostname(),port(onvifPortInput),user(),pass());
            ptz=c;status("Menguji ONVIF GetProfiles ...");
            ptzJobs.execute(()->{
                try{status(c.probe());}
                catch(Exception e){status("ONVIF: "+e.getMessage());}
            });
        }catch(Exception e){status("Periksa IP dan port ONVIF: "+e.getMessage());}
    }
    private void sendMove(int x,int y){
        try{
            final OnvifControl c=control();final float s=speed;
            ptzJobs.execute(()->{
                try{status(c.move(x*s,y*s));}
                catch(Exception e){status("PTZ gagal: "+e.getMessage());}
            });
        }catch(Exception e){status("PTZ input: "+e.getMessage());}
    }
    private void sendStop(){
        try{
            final OnvifControl c=control();
            ptzJobs.execute(()->{
                try{status(c.stop());}
                catch(Exception e){status("PTZ Stop gagal: "+e.getMessage());}
            });
        }catch(Exception e){status("PTZ Stop: "+e.getMessage());}
    }
    private void toggleFullscreen(){
        fullscreen=!fullscreen;
        configuration.setVisibility(fullscreen?View.GONE:View.VISIBLE);
        controls.setVisibility(fullscreen?View.GONE:View.VISIBLE);
        if(fullscreen){
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }else getWindow().getDecorView().setSystemUiVisibility(0);
        ViewGroup.LayoutParams p=screen.getLayoutParams();
        p.height=dp(fullscreen?360:235);
        screen.setLayoutParams(p);
    }
    @Override protected void onStop(){
        super.onStop();stopStream();
        try{final OnvifControl c=ptz;if(c!=null)ptzJobs.execute(()->{try{c.stop();}catch(Exception ignored){}});}
        catch(Exception ignored){}
    }
    @Override protected void onDestroy(){
        super.onDestroy();ptzJobs.shutdownNow();discoveryJobs.shutdownNow();
    }
}
