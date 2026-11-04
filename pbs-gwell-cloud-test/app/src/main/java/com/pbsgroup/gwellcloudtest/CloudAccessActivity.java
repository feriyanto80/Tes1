package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Honest VAS readiness screen. Cloud Playback / AI Event stays unavailable
 * until the vendor confirms the app's product entitlement and supported auth.
 */
public final class CloudAccessActivity extends Activity {
    private final int bg=Color.rgb(10,18,31), panel=Color.rgb(25,36,55),
            white=Color.WHITE, muted=Color.rgb(169,184,200), accent=Color.rgb(36,197,164);
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setPadding(0,dp(6),0,dp(6));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void section(LinearLayout root,String title,String detail,String state){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(shape(panel));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(9);root.addView(card,p);
        card.addView(text(title,15,white,true));card.addView(text(detail,12,muted,false));
        card.addView(text(state,12,accent,true));
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(28));scroll.addView(root);setContentView(scroll);
        root.addView(text("PBS CCTV · CLOUD ACCESS",12,accent,true));
        root.addView(text("Cloud Recording & AI Event",25,white,true));
        root.addView(text("Status integrasi v1.2.9. Halaman ini memeriksa kesiapan akses, bukan menampilkan rekaman yang belum mendapat otorisasi vendor.",13,muted,false));
        section(root,"Yoosee AppID / AppToken",
                "Kredensial aplikasi yang digunakan untuk login Cloudlinks dan mengambil daftar perangkat.",
                "TERSEDIA DI KONFIGURASI LOGIN — validasi dilakukan saat login.");
        section(root,"Device List",
                "Daftar Device ID sudah tersedia melalui alur login akun yang ada.",
                "FITUR EXISTING — dipertahankan dari v1.2.1.");
        section(root,"Gwell IoTVideo Native P2P",
                "Live P2P menggunakan SDK native. Status sesi tetap harus dinilai dari callback PLAY dan frame video.",
                "FITUR EXISTING — tidak diubah oleh modul Cloud.");
        section(root,"Cloud AI Event / Playback VAS",
                "AppID dan AppToken tidak dengan sendirinya membuktikan entitlement VAS. Demo resmi GWTimes memisahkan AccountMgr/secret credentials, product configuration, dan VAS service. Endpoint pada script publik juga memakai signature oracle dari proses aplikasi; APK ini tidak mengambil token dari aplikasi lain atau memakai kredensial milik pihak ketiga.",
                "BELUM AKTIF — menunggu konfirmasi akses VAS dan metode autentikasi resmi dari Gwell.");
        section(root,"Cloud HLS / MP4 Download",
                "URL playback harus diterbitkan oleh layanan cloud untuk akun dan Device ID yang berhak. Tanpa respons VAS yang sah, aplikasi tidak membuat URL atau mengunduh rekaman.",
                "BELUM AKTIF — mengikuti status otorisasi VAS.");
        TextView note=text("Langkah yang dibutuhkan dari Gwell:\n1. Konfirmasi AppID PBS memiliki akses API AI Event dan Cloud Playback.\n2. Berikan dokumentasi autentikasi/signature resmi untuk aplikasi pihak ketiga.\n3. Konfirmasi product/package binding dan hak akses untuk Device ID kamera PBS.\n4. Berikan endpoint resmi, parameter pagination, masa berlaku URL HLS, dan batas download.\n\nJangan mengirim SecretKey, token, atau password melalui chat. Masukkan hanya ke kanal konfigurasi rahasia yang disetujui vendor.",12,white,false);
        note.setPadding(dp(14),dp(14),dp(14),dp(14));note.setBackground(shape(panel));
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(14);root.addView(note,np);
        Button copy=new Button(this);copy.setText("Salin permintaan akses untuk Gwell");copy.setAllCaps(false);
        copy.setOnClickListener(v->{
            String request="Mohon konfirmasi dan aktifkan akses resmi untuk aplikasi PBS (package com.pbsgroup.gwellcloudtest.p2pnative129diag) menggunakan AppID/AppToken PBS: (1) API VAS AI Event list/detail, (2) Cloud Recording playback URL/HLS, (3) product/package binding dan otorisasi Device ID milik PBS, (4) dokumentasi metode autentikasi dan signing resmi untuk aplikasi pihak ketiga, (5) pagination, rate limit, masa berlaku HLS URL, dan ketentuan download. Mohon informasikan apakah diperlukan kredensial partner tambahan di luar AppID/AppToken.";
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Gwell VAS access request",request));
            copy.setText("Permintaan sudah disalin");
        });
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(50));cp.topMargin=dp(12);root.addView(copy,cp);
        Button back=new Button(this);back.setText("Kembali");back.setAllCaps(false);back.setOnClickListener(v->finish());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(8);root.addView(back,bp);
    }
}
