package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import android.view.WindowInsets;
import android.content.ClipData;
import android.content.ClipboardManager;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONArray;

public final class MainActivity extends Activity {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final int background=Color.rgb(10,18,31), panel=Color.rgb(25,36,55),text=Color.WHITE,
        secondary=Color.rgb(169,184,200), accent=Color.rgb(36,197,164);
    private EditText email,password,appId,appToken,mobileArea,deviceId;
    private TextView status;
    private LinearLayout devicesView;
    private final ArrayList<String> deviceIds = new ArrayList<>();
    private YooseeApi api;
    private long lastAttempt=0;
    private String lastReport="Belum ada pengujian.";
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable rounded(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(background);getWindow().setNavigationBarColor(background);
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(background);sc.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(22),dp(18),dp(28));sc.addView(root);setContentView(sc);
        sc.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets a=insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(18),a.top+dp(18),dp(18),a.bottom+dp(30));
            return insets;
        });
        root.addView(label("PBS GROUP · EXPERIMENTAL",12,accent,true));
        root.addView(label("PBS Yoosee Native GWell v1.3.2",27,text,true));
        root.addView(label("Login Cloud API, GWell Native P2P-Core live eksperimental, IoTVideo diagnostics, serta RTSP/ONVIF alternatif.",13,secondary,false));
        section(root,"01 · IDENTITAS APLIKASI VENDOR");
        appId=input(root,"AppID yang sah dari Gwell/Yoosee","",false);
        appToken=input(root,"AppToken yang sah (tidak disimpan)","",true);
        root.addView(label("Nilai AppID/AppToken dari kode pihak ketiga tidak disertakan. Tanpa kredensial aplikasi yang diotorisasi, login dapat ditolak.",11,secondary,false));
        section(root,"02 · AKUN YANG ANDA MILIKI");
        email=input(root,"Email atau nomor telepon","",false);
        password=input(root,"Password akun Yoosee","",true);
        mobileArea=input(root,"Kode negara jika menggunakan nomor HP","+62",false);
        Button login=button(root,"Login sekali ke cloud",true);
        Button list=button(root,"Ambil daftar perangkat akun ini",false);
        devicesView=new LinearLayout(this);
        devicesView.setOrientation(LinearLayout.VERTICAL);
        root.addView(devicesView);
        Button copyIds=button(root,"Salin semua Device ID",false);
        copyIds.setOnClickListener(v->copyAllIds());
        deviceId=input(root,"Device ID untuk melihat metadata (opsional)","",false);
        Button detail=button(root,"Lihat informasi Device ID",false);
        Button liveBtn=button(root,"▶ Buka LIVE VIDEO + PTZ",true);
        liveBtn.setOnClickListener(v->openCameraViewer(deviceId.getText().toString().trim()));
        Button gwellBtn=button(root,"▶ GWell Native P2P-Core LIVE (eksperimental)",true);
        gwellBtn.setOnClickListener(v->openGwellNative(deviceId.getText().toString().trim()));
        Button p2pBtn=button(root,"🔐 Guest P2P Auth & Backend Diagnostic",true);
        p2pBtn.setOnClickListener(v->openNativeP2P(deviceId.getText().toString().trim()));
        Button cloudBtn=button(root,"☁ Cloud Recording / AI Event · cek akses",false);
        cloudBtn.setOnClickListener(v->startActivity(new Intent(this,CloudAccessActivity.class)));
        Button capabilityBtn=button(root,"🧪 API Capability Test",false);
        capabilityBtn.setOnClickListener(v->{Intent test=new Intent(this,ApiCapabilityActivity.class);test.putExtra("device_id",deviceId.getText().toString().trim());startActivity(test);});
        Button endpointInventoryBtn=button(root,"🧭 Endpoint Inventory Scan · HEAD/OPTIONS",false);
        endpointInventoryBtn.setOnClickListener(v->startActivity(new Intent(this,EndpointInventoryActivity.class)));
        Button logout=button(root,"Hapus sesi di memori",false);
        section(root,"03 · HASIL DIAGNOSTIK");
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(14),dp(14),dp(14),dp(14));
        box.setBackground(rounded(panel));
        status=label("Siap. Gunakan hanya akun dan kamera Anda sendiri.",13,text,false);
        box.addView(status);root.addView(box);
        Button share=button(root,"Bagikan laporan tanpa kredensial",false);
        root.addView(label("Keamanan: TLS certificate verification aktif. Tidak ada pencarian kombinasi password, impor daftar akun, penyimpanan token permanen, atau kirim data ke server PBS.",11,secondary,false));
        root.addView(label("Live P2P IoTVideo masih dapat diuji melalui SDK GWTimes. Jalur GWell kini memiliki integrasi SDK native 2019 terpisah, memerlukan vendor APPVersion dan sesi login GWell tersendiri. RTSP+ONVIF tetap tersedia sebagai alternatif.",11,secondary,false));
        login.setOnClickListener(v->runLogin());
        list.setOnClickListener(v->execute("DEVICE_LIST",()->{
            ensureSession();
            JSONObject r=api.listDevices();
            int code=r.optInt("code",-1);
            JSONObject data=r.optJSONObject("data");
            JSONArray arr=data==null?null:data.optJSONArray("deviceList");
            if(arr==null)arr=r.optJSONArray("data");
            int count=arr==null?0:arr.length();
            if(code==0){final JSONArray devices=arr;runOnUiThread(()->showDevices(devices));}
            return "HTTP "+r.optInt("_httpStatus")+" · code "+code+
                (code==0?" · perangkat dalam respons: "+count:
                " · server menolak permintaan / format berubah.")+
                "\nDevice ID dan token tidak dimasukkan ke laporan.";
        }));
        detail.setOnClickListener(v->execute("DEVICE_INFO",()->{
            ensureSession();
            String id=deviceId.getText().toString().trim();
            JSONObject r=api.deviceInfo(id);
            JSONObject data=r.optJSONObject("data");
            String online=data==null?"tidak tersedia":String.valueOf(data.opt("online"));
            return "HTTP "+r.optInt("_httpStatus")+" · code "+r.optInt("code",-1)+
                "\nStatus online (jika tersedia): "+online+"\nMetadata lengkap tidak dicetak untuk menjaga informasi perangkat.";
        }));
        logout.setOnClickListener(v->{
            if(api!=null)api.close();api=null;
            P2PSession.clear();
            devicesView.removeAllViews();deviceIds.clear();
            setStatus("Sesi ditutup dari memori aplikasi.");
        });
        share.setOnClickListener(v->share());
    }

    private void openGwellNative(String id){
        // A blank Device ID must NEVER make the button look unresponsive.
        // The stable launcher opens first and accepts a manually entered ID.
        try{
            Intent launcher=new Intent(this,GwellLauncherActivity.class);
            if(id!=null && id.matches("[A-Za-z0-9_-]{1,100}"))
                launcher.putExtra("device_id",id);
            startActivity(launcher);
        }catch(RuntimeException error){
            String message="GWELL_LAUNCH_ERROR: "+error.getClass().getSimpleName();
            setStatus(message);
            android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();
        }catch(LinkageError error){
            String message="GWELL_SDK_LINK_ERROR: "+error.getClass().getSimpleName();
            setStatus(message);
            android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();
        }
    }
    private void openNativeP2P(String id){
        if(!P2PSession.ready()){
            setStatus("SESSION_MISSING: Login ke Gwell Cloud dulu untuk memperoleh sesi.");
            return;
        }
        if(id==null || id.trim().isEmpty()){
            setStatus("Pilih Device ID kamera terlebih dahulu.");
            return;
        }
        Intent i=new Intent(this,NativeP2PActivity.class);
        i.putExtra("device_id",id);
        startActivity(i);
    }
    private void openCameraViewer(String id){
        Intent i=new Intent(this,CameraViewerActivity.class);
        i.putExtra("device_id",id);
        i.putExtra("rtsp_port","554");
        i.putExtra("rtsp_path","/onvif1");
        i.putExtra("onvif_port","5000");
        startActivity(i);
    }
    private String firstField(JSONObject item,String... fieldNames){
        for(String field:fieldNames){
            String value=item.optString(field,"").trim();
            if(!value.isEmpty() && !value.equalsIgnoreCase("null"))return value;
        }
        return "";
    }
    private void showDevices(JSONArray devices){
        devicesView.removeAllViews();deviceIds.clear();
        if(devices==null || devices.length()==0){
            devicesView.addView(label("Belum ada data perangkat yang dapat ditampilkan.",12,secondary,false));
            return;
        }
        devicesView.addView(label("DAFTAR DEVICE ID ("+devices.length()+")",13,accent,true));
        for(int i=0;i<devices.length();i++){
            JSONObject row=devices.optJSONObject(i);
            if(row==null)continue;
            String id=firstField(row,"deviceId","devId","deviceID","device_id","id");
            String name=firstField(row,"deviceName","devName","name","alias");
            String online=firstField(row,"online","isOnline","status");
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(8),dp(12),dp(8));
            card.setBackground(rounded(panel));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);
            devicesView.addView(card,lp);
            card.addView(label((i+1)+". "+(name.isEmpty()?"Kamera "+(i+1):name),14,text,true));
            TextView idLabel=label(id.isEmpty()?"Device ID: tidak ditemukan":"Device ID: "+id,13,secondary,false);
            idLabel.setTextIsSelectable(true);card.addView(idLabel);
            if(!online.isEmpty())card.addView(label("Status API: "+online,12,secondary,false));
            if(!id.isEmpty()){
                if(!deviceIds.contains(id))deviceIds.add(id);
                final String selected=id;
                button(card,"Pilih dan salin ID",false).setOnClickListener(v->{
                    deviceId.setText(selected);
                    ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                        ClipData.newPlainText("CCTV Device ID",selected));
                    setStatus("Device ID sudah dipilih dan disalin. Tekan 'Lihat informasi Device ID'.");
                });
                button(card,"▶ Live + PTZ",true).setOnClickListener(v->openCameraViewer(selected));
                button(card,"▶ Uji Live P2P native",true).setOnClickListener(v->openNativeP2P(selected));
                button(card,"▶ GWell P2P-Core (pilih kamera)",true).setOnClickListener(v->openGwellNative(selected));
            }else if(row.names()!=null){
                card.addView(label("Field tersedia: "+row.names().toString(),11,secondary,false));
            }
        }
        devicesView.addView(label("Berhasil membaca "+deviceIds.size()+" ID. Daftar hanya ada di HP, tidak dikirim dalam laporan.",12,secondary,false));
    }
    private void copyAllIds(){
        if(deviceIds.isEmpty()){setStatus("Belum ada Device ID untuk disalin.");return;}
        StringBuilder text=new StringBuilder();
        for(String id:deviceIds){if(text.length()>0)text.append("\n");text.append(id);}
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
            ClipData.newPlainText("CCTV Device IDs",text.toString()));
        setStatus(deviceIds.size()+" Device ID disalin ke clipboard.");
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);
        t.setPadding(0,dp(7),0,dp(7));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void section(LinearLayout l,String title){
        TextView t=label(title,13,accent,true);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(18);l.addView(t,p);
    }
    private EditText input(LinearLayout root,String title,String defaultValue,boolean secret){
        root.addView(label(title,12,secondary,false));
        EditText e=new EditText(this);e.setTextColor(text);e.setHintTextColor(secondary);
        e.setTextSize(15);e.setSingleLine(true);e.setText(defaultValue);
        e.setPadding(dp(12),0,dp(12),0);e.setBackground(rounded(panel));
        if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(47));
        p.bottomMargin=dp(5);root.addView(e,p);return e;
    }
    private Button button(LinearLayout root,String caption,boolean primary){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(primary?background:text);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(rounded(primary?accent:panel));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));
        p.topMargin=dp(10);root.addView(b,p);return b;
    }
    private void setStatus(String report){
        lastReport=report;runOnUiThread(()->status.setText(report));
    }
    private void ensureSession(){
        if(api==null||!api.hasSession())throw new IllegalStateException("Belum ada login berhasil.");
    }
    private interface Task {String run() throws Exception;}
    private void execute(String label,Task action){
        setStatus(label+" · menghubungi cloud ...");
        executor.execute(()->{
            try{setStatus(label+"\n"+action.run());}
            catch(Exception e){setStatus(label+"\nGAGAL: "+e.getClass().getSimpleName()+" — "+safeError(e.getMessage()));}
        });
    }
    private String safeError(String msg){
        if(msg==null)return "(tidak ada detail)";
        return msg.length()>160?msg.substring(0,160):msg;
    }
    private void runLogin(){
        long now=System.currentTimeMillis();
        if(now-lastAttempt<30000){setStatus("Uji login dibatasi satu percobaan setiap 30 detik.");return;}
        String id=appId.getText().toString(),t=appToken.getText().toString(),
            account=email.getText().toString().trim(),pw=password.getText().toString(),
            area=mobileArea.getText().toString().trim();
        if(id.trim().isEmpty()||t.trim().isEmpty()||account.isEmpty()||pw.isEmpty()){
            setStatus("Masukkan AppID/AppToken sah, akun dan password. Tidak ada kredensial bawaan.");
            return;
        }
        lastAttempt=now;
        execute("LOGIN",()->{
            if(api!=null)api.close();
            api=new YooseeApi(id,t);
            JSONObject result=api.login(account,pw,area.isEmpty()?"+62":area);
            int code=result.optInt("code",-1);
            if(api.hasSession()){
                P2PSession.save(api.sessionId(),api.sessionToken(),id,t);
                return "HTTP "+result.optInt("_httpStatus")+" · Login akun BERHASIL.\nSesi hanya disimpan di memori. Pilih 'Ambil daftar perangkat akun ini'.";
            }
            P2PSession.clear();
            return "HTTP "+result.optInt("_httpStatus")+" · Login ditolak (code "+code+").\nPeriksa kredensial/izin aplikasi. Tidak mencoba ulang otomatis.";
        });
    }
    private void share(){
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT,"PBS Gwell Cloud Diagnostic");
        i.putExtra(Intent.EXTRA_TEXT,"PBS Gwell API Capability Test v1.3.2\n"+lastReport+
            "\nTidak memuat password, AppToken, P2P Device ID, atau sesi akses.");
        startActivity(Intent.createChooser(i,"Bagikan laporan diagnostik"));
    }
    @Override protected void onDestroy(){
        super.onDestroy();if(api!=null)api.close();executor.shutdownNow();
    }
}
