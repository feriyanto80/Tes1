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
        root.addView(label("PBS GROUP · EXPERIMENTAL",12,accent,true));
        root.addView(label("Gwell Cloud Test",27,text,true));
        root.addView(label("Uji login satu akun pribadi dan daftar kamera Yoosee melalui endpoint cloud tidak resmi. Bukan aplikasi Yoosee resmi atau SDK streaming.",13,secondary,false));
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
        deviceId=input(root,"Device ID untuk melihat metadata (opsional)","",false);
        Button detail=button(root,"Lihat informasi Device ID",false);
        Button logout=button(root,"Hapus sesi di memori",false);
        section(root,"03 · HASIL DIAGNOSTIK");
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(14),dp(14),dp(14),dp(14));
        box.setBackground(rounded(panel));
        status=label("Siap. Gunakan hanya akun dan kamera Anda sendiri.",13,text,false);
        box.addView(status);root.addView(box);
        Button share=button(root,"Bagikan laporan tanpa kredensial",false);
        root.addView(label("Keamanan: TLS certificate verification aktif. Tidak ada pencarian kombinasi password, impor daftar akun, penyimpanan token permanen, atau kirim data ke server PBS.",11,secondary,false));
        root.addView(label("Untuk video P2P, masih diperlukan SDK streaming vendor dan lisensi yang sesuai. API login ini tidak menghasilkan URL RTSP.",11,secondary,false));
        login.setOnClickListener(v->runLogin());
        list.setOnClickListener(v->execute("DEVICE_LIST",()->{
            ensureSession();
            JSONObject r=api.listDevices();
            int code=r.optInt("code",-1);
            JSONObject data=r.optJSONObject("data");
            JSONArray arr=data==null?null:data.optJSONArray("deviceList");
            if(arr==null)arr=r.optJSONArray("data");
            int count=arr==null?0:arr.length();
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
            setStatus("Sesi ditutup dari memori aplikasi.");
        });
        share.setOnClickListener(v->share());
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
        if(id.isBlank()||t.isBlank()||account.isBlank()||pw.isBlank()){
            setStatus("Masukkan AppID/AppToken sah, akun dan password. Tidak ada kredensial bawaan.");
            return;
        }
        lastAttempt=now;
        execute("LOGIN",()->{
            if(api!=null)api.close();
            api=new YooseeApi(id,t);
            JSONObject result=api.login(account,pw,area.isEmpty()?"+62":area);
            int code=result.optInt("code",-1);
            if(api.hasSession())
                return "HTTP "+result.optInt("_httpStatus")+" · Login akun BERHASIL.\nSesi hanya disimpan di memori. Pilih 'Ambil daftar perangkat akun ini'.";
            return "HTTP "+result.optInt("_httpStatus")+" · Login ditolak (code "+code+").\nPeriksa kredensial/izin aplikasi. Tidak mencoba ulang otomatis.";
        });
    }
    private void share(){
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT,"PBS Gwell Cloud Diagnostic");
        i.putExtra(Intent.EXTRA_TEXT,"PBS Gwell Cloud Diagnostic v1.0.0\n"+lastReport+
            "\nTidak memuat password, AppToken, P2P Device ID, atau sesi akses.");
        startActivity(Intent.createChooser(i,"Bagikan laporan diagnostik"));
    }
    @Override protected void onDestroy(){
        super.onDestroy();if(api!=null)api.close();executor.shutdownNow();
    }
}
