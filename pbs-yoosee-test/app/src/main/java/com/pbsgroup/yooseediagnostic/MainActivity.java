package com.pbsgroup.yooseediagnostic;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public final class MainActivity extends Activity {
    private final int bg=Color.rgb(11,18,32), panel=Color.rgb(23,34,53);
    private final int accent=Color.rgb(33,191,153), white=Color.WHITE, muted=Color.rgb(172,184,200);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private EditText ip,port,username,password,rtspPath,p2pId;
    private TextView result,network,cloud;
    private PlayerView playerView;
    private ExoPlayer player;
    private String lastOnvif="Belum diuji", lastRtsp="Belum diuji", lastNetwork="Belum diuji";
    private int dp(float x){return (int)(getResources().getDisplayMetrics().density*x+0.5f);}
    private GradientDrawable shape(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(18),dp(20),dp(18),dp(32));
        scroll.addView(main);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets a=insets.getInsets(WindowInsets.Type.systemBars());
            main.setPadding(dp(18),a.top+dp(20),dp(18),a.bottom+dp(32));
            return insets;
        });
        setContentView(scroll);
        TextView heading=label("PBS · YO OSEE TEST",13,accent,true);
        main.addView(heading);
        main.addView(label("Camera Diagnostic",27,white,true));
        main.addView(label("Uji kamera milik sendiri melalui LAN / VPN. Tes P2P Cloud memerlukan SDK vendor resmi.",13,muted,false));
        section(main,"01 · KONEKSI KAMERA");
        ip=input(main,"Alamat IP kamera","192.168.1.100",false);
        port=input(main,"Port ONVIF (80 atau 5000)","5000",false);
        username=input(main,"Username kamera","admin",false);
        password=input(main,"Password kamera","",true);
        rtspPath=input(main,"RTSP path (model-specific)","/onvif1",false);
        p2pId=input(main,"P2P Device ID (referensi saja)","",false);
        Button networkBtn=button(main,"Periksa jaringan HP",false);
        Button onvifBtn=button(main,"Uji ONVIF Device Service",true);
        Button streamBtn=button(main,"Putar Live RTSP (TCP)",true);
        Button stopBtn=button(main,"Hentikan video",false);
        network=label("Jaringan: belum diuji",12,muted,false);main.addView(network);
        result=label("Hasil ONVIF/RTSP akan ditampilkan di sini.",12,white,false);
        LinearLayout resultBox=card(main);resultBox.addView(result);
        playerView=new PlayerView(this);
        playerView.setUseController(true);
        playerView.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams videoParams=new LinearLayout.LayoutParams(-1,dp(200));
        videoParams.topMargin=dp(12);main.addView(playerView,videoParams);
        section(main,"02 · GWELL / YOOSEE P2P CLOUD");
        LinearLayout cloudBox=card(main);
        cloud=label("STATUS: SDK P2P BELUM TERPASANG. AppID/AppToken resmi belum tersedia. Device ID tidak cukup untuk membuka stream.",13,muted,false);
        cloudBox.addView(cloud);
        button(main,"Buka aplikasi Yoosee resmi",false).setOnClickListener(v->openYoosee());
        section(main,"03 · LAPORAN UJI");
        button(main,"Bagikan hasil diagnostik",false).setOnClickListener(v->shareReport());
        main.addView(label("Data username/password tidak dikirim ke server dan tidak disertakan dalam laporan. APP INI TIDAK MENGAKSES CLOUD GWELL.",11,muted,false));
        networkBtn.setOnClickListener(v->checkNetwork());
        onvifBtn.setOnClickListener(v->testOnvif());
        streamBtn.setOnClickListener(v->playRtsp());
        stopBtn.setOnClickListener(v->stopPlayer());
        checkNetwork();
    }
    private TextView label(String value,int sp,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setPadding(0,dp(6),0,dp(7));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void section(LinearLayout root,String title){
        TextView t=label(title,13,accent,true);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(15);root.addView(t,p);
    }
    private LinearLayout card(LinearLayout root){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(12),dp(14),dp(12));
        l.setBackground(shape(panel));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(10);root.addView(l,p);return l;
    }
    private EditText input(LinearLayout root,String hint,String value,boolean isSecret){
        TextView t=label(hint,12,muted,false);root.addView(t);
        EditText e=new EditText(this);e.setText(value);e.setSingleLine(true);
        e.setTextSize(15);e.setTextColor(white);e.setHintTextColor(muted);
        e.setPadding(dp(12),0,dp(12),0);
        e.setBackground(shape(panel));
        if(isSecret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));
        p.bottomMargin=dp(6);root.addView(e,p);return e;
    }
    private Button button(LinearLayout root,String value,boolean primary){
        Button b=new Button(this);
        b.setText(value);b.setTextSize(13);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?bg:white);b.setBackground(shape(primary?accent:panel));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(47));p.topMargin=dp(9);
        root.addView(b,p);return b;
    }
    private String host(){
        String s=ip.getText().toString().trim();
        if(!s.matches("[A-Za-z0-9.\\-]{1,255}"))throw new IllegalArgumentException("Alamat IP / hostname tidak valid.");
        return s;
    }
    private int onvifPort(){
        int n=Integer.parseInt(port.getText().toString().trim());
        if(n<1||n>65535)throw new IllegalArgumentException("Port harus 1–65535");
        return n;
    }
    private void status(String s){runOnUiThread(()->result.setText(s));}
    private void checkNetwork(){
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkCapabilities nc=cm.getNetworkCapabilities(cm.getActiveNetwork());
        String kind=nc==null?"Tidak ada koneksi":nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"WiFi":nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Seluler":nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"Jaringan lainnya";
        lastNetwork=kind;network.setText("Jaringan HP: "+kind+" · Kamera harus terjangkau melalui LAN/VPN untuk RTSP.");
    }
    private void testOnvif(){
        final String h,u,pw;final int p;
        try{h=host();p=onvifPort();u=username.getText().toString();pw=password.getText().toString();}
        catch(Exception e){status("Input: "+e.getMessage());return;}
        status("Menghubungi ONVIF pada "+h+":"+p+" ...");
        io.execute(()->{
            try{
                String created=utcNow(), token=wsSecurity(u,pw,created);
                String xml="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                    "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\""+
                    " xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\""+
                    " xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""+
                    " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"+
                    "<s:Header>"+token+"</s:Header><s:Body><tds:GetDeviceInformation/></s:Body></s:Envelope>";
                URL url=new URL("http://"+h+":"+p+"/onvif/device_service");
                HttpURLConnection c=(HttpURLConnection)url.openConnection();
                c.setRequestMethod("POST");c.setConnectTimeout(6000);c.setReadTimeout(7000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type","application/soap+xml; charset=utf-8");
                c.setRequestProperty("Connection","close");
                byte[] bytes=xml.getBytes(StandardCharsets.UTF_8);
                try(OutputStream out=c.getOutputStream()){out.write(bytes);}
                int code=c.getResponseCode();
                InputStream stream=code>=400?c.getErrorStream():c.getInputStream();
                byte[] data=readUpTo(stream,65536);
                String response=new String(data,StandardCharsets.UTF_8);
                String msg="HTTP "+code+" · "+(code==200?"Respons ONVIF diterima.":"Periksa port, username/password dan pengaturan ONVIF.");
                if(code==200){
                    String model=xmlTag(response,"Model"),manufacturer=xmlTag(response,"Manufacturer"),serial=xmlTag(response,"SerialNumber");
                    if(model!=null)msg+="\nModel: "+model;
                    if(manufacturer!=null)msg+="\nVendor: "+manufacturer;
                    if(serial!=null)msg+="\nSerial: "+mask(serial);
                    if(response.contains("Fault"))msg+="\nSOAP Fault dikembalikan kamera.";
                } else if(code==401)msg+="\nKamera meminta otorisasi HTTP lain (Digest/Basic).";
                lastOnvif=msg;status("ONVIF: "+msg);
                c.disconnect();
            }catch(Exception e){
                lastOnvif="GAGAL: "+e.getClass().getSimpleName()+" · "+String.valueOf(e.getMessage());
                status("ONVIF "+lastOnvif+"\nPastikan HP berada di WiFi yang sama atau VPN tersambung.");
            }
        });
    }
    private static String utcNow(){
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));return df.format(new Date());
    }
    private static String wsSecurity(String username,String password,String created) throws Exception {
        byte[] nonce=new byte[16];new SecureRandom().nextBytes(nonce);
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        b.write(nonce);b.write(created.getBytes(StandardCharsets.UTF_8));b.write(password.getBytes(StandardCharsets.UTF_8));
        byte[] digest=MessageDigest.getInstance("SHA-1").digest(b.toByteArray());
        return "<wsse:Security s:mustUnderstand=\"1\"><wsse:UsernameToken>"+
            "<wsse:Username>"+escapeXml(username)+"</wsse:Username>"+
            "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">"+
            Base64.getEncoder().encodeToString(digest)+"</wsse:Password>"+
            "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">"+
            Base64.getEncoder().encodeToString(nonce)+"</wsse:Nonce><wsu:Created>"+created+
            "</wsu:Created></wsse:UsernameToken></wsse:Security>";
    }
    private static String escapeXml(String s){
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");
    }
    private static byte[] readUpTo(InputStream stream,int limit)throws Exception{
        if(stream==null)return new byte[0];
        try(InputStream in=stream;ByteArrayOutputStream b=new ByteArrayOutputStream()){
            byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1){if(b.size()+n>limit)throw new IllegalArgumentException("Respon ONVIF terlalu besar.");b.write(buf,0,n);}
            return b.toByteArray();
        }
    }
    private static String xmlTag(String xml,String localName){
        try{
            DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            f.setFeature("http://xml.org/sax/features/external-general-entities",false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            Document d=f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes=d.getElementsByTagNameNS("*",localName);
            if(nodes.getLength()>0)return nodes.item(0).getTextContent().trim();
        }catch(Exception ignored){}
        return null;
    }
    private static String mask(String value){
        if(value.length()<=4)return "****";
        return "****"+value.substring(value.length()-4);
    }
    private void playRtsp(){
        try{
            String h=host(),u=username.getText().toString(),pw=password.getText().toString();
            String path=rtspPath.getText().toString().trim();
            if(!path.startsWith("/"))path="/"+path;
            String auth=u.isEmpty()?"":Uri.encode(u)+":"+Uri.encode(pw)+"@";
            String url="rtsp://"+auth+h+":554"+path;
            stopPlayer();
            player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);
            player.addListener(new Player.Listener(){
                @Override public void onPlaybackStateChanged(int state){
                    if(state==Player.STATE_READY){lastRtsp="BERHASIL - video siap diputar";status("RTSP: BERHASIL (kamera terjangkau).");}
                    if(state==Player.STATE_BUFFERING)status("RTSP: menghubungkan / buffering ...");
                }
                @Override public void onPlayerError(PlaybackException e){
                    lastRtsp="GAGAL: "+e.getErrorCodeName();
                    status("RTSP "+lastRtsp+"\nCek URL stream model kamera, kredensial, dan rute WiFi/VPN.");
                }
            });
            RtspMediaSource source=new RtspMediaSource.Factory()
                .setForceUseRtpTcp(true).createMediaSource(MediaItem.fromUri(url));
            player.setMediaSource(source);player.prepare();player.play();
            lastRtsp="Menghubungkan ...";
        }catch(Exception e){
            lastRtsp="Input salah: "+e.getClass().getSimpleName();
            status("RTSP "+lastRtsp);
        }
    }
    private void stopPlayer(){
        if(player!=null){player.stop();player.release();player=null;}
        if(playerView!=null)playerView.setPlayer(null);
    }
    private void openYoosee(){
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.yoosee");
        if(launch!=null){startActivity(launch);return;}
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.yoosee")));}
        catch(ActivityNotFoundException e){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.yoosee")));}
    }
    private void shareReport(){
        String report="PBS YOOSEE DIAGNOSTIC v1.0.0\nTanggal: "+new Date()+
            "\nJaringan: "+lastNetwork+"\nONVIF: "+lastOnvif+"\nRTSP: "+lastRtsp+
            "\nP2P SDK: BELUM TERPASANG - memerlukan AppID/AppToken terotorisasi."+
            "\nPassword dan P2P Device ID tidak dicantumkan.\n";
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT,"PBS Yoosee Diagnostic");
        send.putExtra(Intent.EXTRA_TEXT,report);
        startActivity(Intent.createChooser(send,"Bagikan laporan"));
    }
    @Override protected void onStop(){super.onStop();stopPlayer();}
    @Override protected void onDestroy(){super.onDestroy();io.shutdownNow();}
}
