package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Stable launcher that does not load native GWell SDK classes on open.
 *
 * v1.3.0 silently returned to the main page when the Device ID input was
 * blank; the diagnostic status was off-screen.  This screen always opens,
 * accepts a manually supplied ID, and reports failed Activity resolution.
 */
public final class GwellLauncherActivity extends Activity {
    private final int bg=Color.rgb(10,18,31),panel=Color.rgb(25,37,57),
       white=Color.WHITE,muted=Color.rgb(173,186,204),accent=Color.rgb(32,196,162);
    private EditText deviceInput;
    private TextView diagnostic;
    private String lastDiagnostic="NO_ATTEMPT";
    private boolean launchedNative=false;

    private int dp(int n){
        return (int)(n*getResources().getDisplayMetrics().density+0.5f);
    }
    private GradientDrawable rounded(int color){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        return d;
    }
    private TextView label(String value,int sp,int color,boolean strong){
        TextView t=new TextView(this);
        t.setText(value); t.setTextColor(color);t.setTextSize(sp);
        t.setPadding(0,dp(6),0,dp(7));
        if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(LinearLayout root,String value,boolean primary){
        Button b=new Button(this);
        b.setText(value);b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?bg:white);
        b.setBackground(rounded(primary?accent:panel));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));
        lp.topMargin=dp(10);
        root.addView(b,lp);
        return b;
    }
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);sc.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(32));
        sc.addView(root);setContentView(sc);
        sc.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(18),bars.top+dp(20),dp(18),bars.bottom+dp(32));
            return insets;
        });

        root.addView(label("PBS GROUP · GWell P2P-Core",12,accent,true));
        root.addView(label("Native GWell Live",24,white,true));
        root.addView(label("v1.3.3 · Receiver compatibility Android 14/16 dan proses GWell terisolasi. Jika runtime SDK lama crash, layar utama PBS tetap berjalan dan laporan crash dapat disalin.",13,muted,false));
        root.addView(label("Device ID kamera",13,white,true));

        deviceInput=new EditText(this);
        deviceInput.setSingleLine(true);
        deviceInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        deviceInput.setTextColor(white);deviceInput.setHintTextColor(muted);
        deviceInput.setHint("Isi Device ID, contoh 38819486");
        deviceInput.setBackground(rounded(panel));
        deviceInput.setPadding(dp(14),0,dp(14),0);
        LinearLayout.LayoutParams entry=new LinearLayout.LayoutParams(-1,dp(52));
        root.addView(deviceInput,entry);
        String supplied=getIntent().getStringExtra("device_id");
        if(supplied!=null && supplied.matches("[A-Za-z0-9_-]{1,100}"))
            deviceInput.setText(supplied);

        Button launch=button(root,"▶ Buka GWell Native Player",true);
        launch.setOnClickListener(v->launchNative());
        Button copy=button(root,"Salin diagnostik & crash terakhir",false);
        copy.setOnClickListener(v->copyDiagnostic());
        diagnostic=label("READY: layar launcher berhasil dibuka. Masukkan/pastikan Device ID, lalu buka native player.",13,white,false);
        diagnostic.setTextIsSelectable(true);
        root.addView(diagnostic);
        root.addView(label("Status proses sebelumnya: "+NativeCrashLedger.report(this),11,muted,false));
        root.addView(label("Catatan: 8003 pada SDK IoTVideo tidak otomatis berlaku untuk SDK GWell. Tidak ada binding/unbinding kamera atau pengambilalihan sesi Yoosee.",12,muted,false));
    }
    private void report(String state){
        lastDiagnostic=state;
        if(diagnostic!=null)diagnostic.setText(state);
        Toast.makeText(this,state,Toast.LENGTH_LONG).show();
    }
    private void launchNative(){
        String id=deviceInput.getText().toString().trim();
        if(id.isEmpty()){
            report("DEVICE_ID_EMPTY: isi Device ID sebelum menjalankan GWell native.");
            deviceInput.requestFocus();
            return;
        }
        if(!id.matches("[A-Za-z0-9_-]{1,100}")){
            report("DEVICE_ID_INVALID: hanya huruf, angka, _ atau - (maks. 100).");
            deviceInput.requestFocus();
            return;
        }
        try{
            ComponentName component=new ComponentName(getPackageName(),
                "com.pbsgroup.gwellcloudtest.GwellNativeActivity");
            getPackageManager().getActivityInfo(component,0);
            Intent intent=new Intent();
            intent.setComponent(component);
            intent.putExtra("device_id",id);
            NativeCrashLedger.launching(this);
            launchedNative=true;
            report("NATIVE_LAUNCH_REQUESTED: memulai SDK GWell dalam proses terpisah. Kembali ke layar ini jika native gagal.");
            startActivity(intent);
        }catch(PackageManager.NameNotFoundException error){
            report("NATIVE_ACTIVITY_MISSING: GwellNativeActivity tidak terdaftar di APK.");
        }catch(LinkageError error){
            report("NATIVE_DEPENDENCY_ERROR: "+error.getClass().getSimpleName()+
                    ". Periksa AAR GWell dan ABI Android.");
        }catch(RuntimeException error){
            report("NATIVE_ACTIVITY_LAUNCH_FAILED: "+error.getClass().getSimpleName());
        }
    }
    @Override protected void onResume(){
        super.onResume();
        if(launchedNative && diagnostic!=null){
            lastDiagnostic="RETURNED_FROM_NATIVE: inspect the latest native process state and Java crash report";
            diagnostic.setText(lastDiagnostic+"\n"+NativeCrashLedger.report(this));
        }
    }
    private void copyDiagnostic(){
        String status="PBS GWell Native Launcher v1.3.3"+
            "\nstatus="+lastDiagnostic+
            "\nactivityPackage="+getPackageName()+
            "\ndeviceIdPresent="+(deviceInput!=null&&!deviceInput.getText().toString().trim().isEmpty())+
            "\ncredentialsExcluded=true\n"+NativeCrashLedger.report(this);
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
            ClipData.newPlainText("GWell launcher diagnostic",status));
        Toast.makeText(this,"Diagnostik launcher disalin tanpa kredensial.",Toast.LENGTH_SHORT).show();
    }
}
