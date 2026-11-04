package com.pbsgroup.gwellcloudtest;

import android.content.ClipData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.libhttp.entity.LoginGResult;
import com.libhttp.subscribers.SubscriberListener;
import com.p2p.core.BaseMonitorActivity;
import com.p2p.core.MediaPlayer;
import com.p2p.core.P2PHandler;
import com.p2p.core.P2PInterface.IP2P;
import com.p2p.core.P2PInterface.ISetting;
import com.p2p.core.P2PSpecial.HttpErrorCode;
import com.p2p.core.P2PSpecial.HttpSend;
import com.p2p.core.P2PSpecial.P2PSpecial;
import com.p2p.core.P2PView;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Experimental vendor GWell P2P-Core SDK integration based on jwkj/GwellDemo.
 * Vendor app registration and an authorized login are mandatory.
 * Credentials, session IDs, device passwords and raw callback payloads
 * are never exported or stored persistently.
 */
public final class GwellNativeActivity extends BaseMonitorActivity {
    private final int bg=Color.rgb(10,18,31),panel=Color.rgb(25,37,57),
        white=Color.WHITE,muted=Color.rgb(179,189,203),accent=Color.rgb(32,196,162);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final StringBuilder log=new StringBuilder();

    private EditText appIdInput,appTokenInput,appVersionInput,accountInput,
                     passwordInput,devicePasswordInput;
    private TextView stateView;
    private RelativeLayout videoHost;
    private String deviceId="";
    private String userId="";
    private String lastOutcome="NOT_STARTED";
    private volatile boolean sdkInitialized=false;
    private volatile boolean loginAccepted=false;
    private volatile boolean p2pConnected=false;
    private volatile boolean versionKnown=false;
    private volatile boolean monitorAccepted=false;
    private volatile boolean monitorReady=false;
    private volatile boolean videoPtsSeen=false;
    private volatile int channelId=-1;
    private IP2P p2pListener;
    private ISetting settingListener;
    private int generation=0;
    private boolean closed=false;

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable rounded(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;
    }
    private TextView label(String title,int size,int color){
        TextView t=new TextView(this);t.setText(title);t.setTextColor(color);
        t.setTextSize(size);t.setPadding(dp(3),dp(6),dp(3),dp(6));return t;
    }
    private Button button(LinearLayout root,String title,boolean primary,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);
        b.setTextColor(primary?bg:white);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextSize(13);b.setBackground(rounded(primary?accent:panel));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.topMargin=dp(8);
        root.addView(b,lp);b.setOnClickListener(v->action.run());return b;
    }
    private EditText field(LinearLayout root,String hint,String initial,boolean secret){
        root.addView(label(hint,12,muted));
        EditText f=new EditText(this);f.setTextColor(white);
        f.setHintTextColor(muted);f.setTextSize(14);
        f.setSingleLine(true);f.setText(initial);
        f.setPadding(dp(12),0,dp(12),0);f.setBackground(rounded(panel));
        f.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:
            InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(49));lp.bottomMargin=dp(7);
        root.addView(f,lp);return f;
    }
    private void event(String message){
        synchronized(log){
            if(log.length()>9500)log.delete(0,log.length()-6500);
            log.append(System.currentTimeMillis()).append(" | ").append(message).append("\n");
        }
        lastOutcome=message;
        runOnUiThread(()->{
            if(!closed && stateView!=null)stateView.setText(message);
        });
    }
    private String maskedDevice(){
        if(deviceId.length()<5)return "[REDACTED]";
        return deviceId.substring(0,3)+"…"+deviceId.substring(deviceId.length()-2);
    }
    // Legacy GWell AAR still calls pre-Android-14 registerReceiver overloads
    // from its BaseMonitorActivity setup. Do not broadly suppress errors.
    // Retry ONLY the platform's missing export-flag SecurityException.
    @Override public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter){
        try{
            return super.registerReceiver(receiver,filter);
        }catch(SecurityException missingFlag){
            if(android.os.Build.VERSION.SDK_INT < 34 || receiver==null ||
               !GwellReceiverCompat.isMissingExportFlag(missingFlag))throw missingFlag;
            Intent result=super.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);
            NativeCrashLedger.receiverCompatApplied(this,"Activity");
            return result;
        }
    }

    @Override public Intent registerReceiver(BroadcastReceiver receiver,IntentFilter filter,
                                             String permission,Handler scheduler){
        try{
            return super.registerReceiver(receiver,filter,permission,scheduler);
        }catch(SecurityException missingFlag){
            if(android.os.Build.VERSION.SDK_INT < 34 || receiver==null ||
               !GwellReceiverCompat.isMissingExportFlag(missingFlag))throw missingFlag;
            Intent result=super.registerReceiver(receiver,filter,permission,scheduler,
                Context.RECEIVER_NOT_EXPORTED);
            NativeCrashLedger.receiverCompatApplied(this,"Activity");
            return result;
        }
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        NativeCrashLedger.opened(this);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        deviceId=getIntent().getStringExtra("device_id");
        if(deviceId==null)deviceId="";
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(30));
        scroll.addView(root);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(16),bars.top+dp(20),dp(16),bars.bottom+dp(30));
            return insets;
        });
        root.addView(label("PBS · GWell P2P-CORE v1.3.3",13,accent));
        root.addView(label("Native GWell Live · Vendor SDK",23,white));
        root.addView(label("Device ID: "+deviceId,14,muted));
        root.addView(label("SDK nyata dari jwkj/GwellDemo. Login native GWell memerlukan kredensial aplikasi dengan APPVersion yang didaftarkan vendor. Login Cloudlinks PBS tidak otomatis menyediakan SessionID1/2 dan P2PVerifyCode1/2.",12,muted));

        appIdInput=field(root,"GWell AppID terdaftar",P2PSession.appId()==null?"":P2PSession.appId(),false);
        appTokenInput=field(root,"GWell AppToken terdaftar",P2PSession.appToken()==null?"":P2PSession.appToken(),true);
        appVersionInput=field(root,"APPVersion dari vendor (mis. XX.XX.XX.XX)","",false);
        accountInput=field(root,"Email / nomor +kode negara / UserID akun GWell","",false);
        passwordInput=field(root,"Password akun GWell (hanya memori)", "",true);
        devicePasswordInput=field(root,"Password monitor kamera, jika diberikan pemilik","",true);

        videoHost=new RelativeLayout(this);
        videoHost.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams videoParams=new LinearLayout.LayoutParams(-1,dp(260));
        videoParams.topMargin=dp(12);
        root.addView(videoHost,videoParams);
        stateView=label("Belum login native GWell.",13,white);
        stateView.setTextIsSelectable(true);root.addView(stateView);

        button(root,"1 · Init GWell SDK & login akun",true,this::startGwellLogin);
        button(root,"2 · Periksa P2P Device & Versi",false,this::requestDeviceStatus);
        button(root,"3 · Mulai Live GWell P2P",true,this::startMonitor);
        button(root,"■ Hentikan sesi GWell",false,this::stopMonitor);
        button(root,"Salin laporan (tanpa kredensial)",false,this::copyReport);
        root.addView(label("PENTING: kode APPVersion bukan Android versionName. Identitas APPID/APPTOKEN, APPVersion, dan package name mungkin terikat pada vendor. Paket ini berisi SDK contoh arsip 2019; bukan bypass akses.",11,muted));
        root.addView(label("SDK mengembalikan status koneksi, versi P2P, vAccept/vConnectReady, dan video PTS. Koneksi sukses belum menjamin video muncul. Tidak melakukan bind/unbind, reset, atau mengambil token Yoosee resmi.",11,muted));
        root.addView(label("Android 14+: registration broadcast internal SDK yang belum mencantumkan export flag dicoba ulang sebagai NOT_EXPORTED. Mesin native berjalan di proses Android terpisah. Jika proses berhenti, kembali ke launcher untuk menyalin diagnostik. Kredensial Cloudlinks dari proses utama tidak otomatis tersedia di proses terpisah.",11,muted));
        NativeCrashLedger.uiReady(this);
    }

    private String get(EditText f){return f.getText().toString().trim();}
    private void startGwellLogin(){
        if(sdkInitialized || loginAccepted){
            event("SESSION_ALREADY_INITIALIZED: jangan melakukan init/login ulang pada sesi sama. Buka ulang layar untuk login baru.");
            return;
        }
        String appId=get(appIdInput),appToken=get(appTokenInput),version=get(appVersionInput),
            account=get(accountInput),password=passwordInput.getText().toString();
        if(appId.isEmpty() || appToken.isEmpty() || account.isEmpty() || password.isEmpty()){
            event("REQUIRED_FIELDS_MISSING: AppID, AppToken, akun dan password diperlukan.");
            return;
        }
        if(!version.matches("[0-9]{2}\\.[0-9]{2}\\.[0-9]{2}\\.[0-9]{2}")){
            event("APPVERSION_INVALID: gunakan format vendor XX.XX.XX.XX, jangan Android versionName.");
            return;
        }
        try{
            P2PSpecial.getInstance().init(getApplication(),appId,appToken,version);
            sdkInitialized=true;
            buildVideoView();
            event("GWELL_SDK_INITIALIZED: AppID/AppToken/APPVersion submitted; server-side application authorization NOT proven");
        }catch(Throwable t){
            event("GWELL_INIT_FAILED: "+t.getClass().getSimpleName()+"; no credentials exported");
            return;
        }
        // Old SDK login is callback-based; never retry automatically to avoid throttling.
        final int id=++generation;
        try{
            HttpSend.getInstance().login(account,password,"",new SubscriberListener<LoginGResult>(){
                @Override public void onStart(){event("GWELL_LOGIN_REQUESTED: user credentials redacted");}
                @Override public void onNext(LoginGResult result){
                    if(id!=generation||closed)return;
                    if(result==null){event("GWELL_LOGIN_EMPTY");return;}
                    String code=result.getError_code();
                    if(!HttpErrorCode.ERROR_0.equals(code)){
                        event("GWELL_LOGIN_REJECTED: vendorCode="+safeCode(code)+"; APPVersion/account/app binding should be verified");
                        return;
                    }
                    try{
                        userId=result.getUserID();
                        long session1=Long.parseLong(result.getSessionID());
                        long session2=Long.parseLong(result.getSessionID2());
                        int code1=Integer.parseInt(result.getP2PVerifyCode1());
                        int code2=Integer.parseInt(result.getP2PVerifyCode2());
                        if(userId==null||userId.trim().isEmpty())throw new IllegalArgumentException("UserID missing");
                        loginAccepted=true;
                        event("GWELL_LOGIN_OK: UserID, SessionID1/2, P2PVerifyCode1/2 received; no values logged");
                        initNativeP2p();
                        worker.execute(()->connectP2p(id,userId,session1,session2,code1,code2));
                    }catch(Throwable t){
                        event("GWELL_LOGIN_SESSION_PARSE_FAILED: "+t.getClass().getSimpleName());
                    }
                }
                @Override public void onError(String code,Throwable error){
                    event("GWELL_LOGIN_ERROR: vendorCode="+safeCode(code)+
                        "; cause="+(error==null?"UNKNOWN":error.getClass().getSimpleName()));
                }
            });
        }catch(Throwable t){
            event("GWELL_LOGIN_CALL_FAILED: "+t.getClass().getSimpleName());
        }
    }
    private static String safeCode(String value){
        if(value==null)return "UNKNOWN";
        return value.matches("[0-9A-Za-z_-]{1,32}")?value:"REDACTED";
    }
    private void buildVideoView(){
        if(pView!=null)return;
        try{
            getLayoutInflater().inflate(R.layout.pbs_gwell_native_view,videoHost,true);
            pView=videoHost.findViewById(R.id.pbs_gwell_video);
            initP2PView(7,P2PView.LAYOUTTYPE_TOGGEDER);
        }catch(Throwable t){
            event("GWELL_P2P_VIEW_ERROR: "+t.getClass().getSimpleName());
        }
    }
    private void initNativeP2p(){
        if(p2pListener!=null)return;
        InvocationHandler p2pHandler=(proxy,method,args)->{
            String name=method.getName();
            if("toString".equals(name))return "PBS_IP2P_Listener";
            if("hashCode".equals(name))return System.identityHashCode(proxy);
            if("equals".equals(name))return proxy==args[0];
            if("vAccept".equals(name) && args!=null && args.length>=3){
                int type=((Number)args[0]).intValue(),scale=((Number)args[1]).intValue(),
                    ch=((Number)args[2]).intValue();
                runOnUiThread(()->{
                    monitorAccepted=true;channelId=ch;
                    event("GWELL_V_ACCEPT: channel assigned; media not yet verified");
                    try {
                        if(pView!=null){pView.setType(type);pView.setScale(scale);pView.changeWindow(ch);}
                        P2PHandler.getInstance().openAudioAndStartPlaying(ch,1);
                    }catch(Throwable t){event("GWELL_START_PLAYER_ERROR: "+t.getClass().getSimpleName());}
                });
            }else if("vConnectReady".equals(name) && args!=null && args.length>=1){
                int ch=((Number)args[0]).intValue();
                runOnUiThread(()->{
                    channelId=ch;monitorReady=true;
                    event("GWELL_MONITOR_READY: signaling ready, awaiting video PTS");
                    try{if(pView!=null)pView.sendStartBrod(ch);}
                    catch(Throwable t){event("GWELL_START_BROADCAST_ERROR: "+t.getClass().getSimpleName());}
                });
            }else if("vReject".equals(name)){
                int reason=args!=null&&args.length>1&&args[1] instanceof Number?
                    ((Number)args[1]).intValue():-1;
                int ex1=args!=null&&args.length>2&&args[2] instanceof Number?
                    ((Number)args[2]).intValue():-1;
                event("GWELL_MONITOR_REJECTED: reason="+reason+"; exCode1="+ex1+
                    "; device credentials, guest permission or backend may be incompatible");
            }
            return defaultValue(method.getReturnType());
        };
        InvocationHandler settingHandler=(proxy,method,args)->{
            String name=method.getName();
            if("toString".equals(name))return "PBS_ISetting_Listener";
            if("hashCode".equals(name))return System.identityHashCode(proxy);
            if("equals".equals(name))return proxy==args[0];
            if("vRetGetIndexFriendStatus".equals(name) && args!=null){
                try{
                    String[] ids=(String[])args[1];
                    short[] versions=(short[])args[args.length-1];
                    if(ids!=null && versions!=null){
                        int count=Math.min(ids.length,versions.length);
                        int[] devices=new int[count];
                        for(int i=0;i<count;i++){
                            devices[i]=Integer.parseInt(ids[i]);
                            if(ids[i].startsWith("0"))devices[i]|=0x80000000;
                        }
                        if(count>0) MediaPlayer.setP2PLibVersion(devices,versions,count);
                        for(int i=0;i<count;i++)if(deviceId.equals(ids[i])){
                            versionKnown=true;
                            event("GWELL_DEVICE_VERSION_VERIFIED: received version metadata from vendor, safe to start monitor");
                            break;
                        }
                        if(!versionKnown)event("GWELL_DEVICE_VERSION_NOT_IN_LIST: no confirmed compatibility version");
                    }
                }catch(Throwable t){event("GWELL_DEVICE_STATUS_PARSE_ERROR: "+t.getClass().getSimpleName());}
            }
            return defaultValue(method.getReturnType());
        };
        try{
            p2pListener=(IP2P)Proxy.newProxyInstance(IP2P.class.getClassLoader(),
                new Class[]{IP2P.class},p2pHandler);
            settingListener=(ISetting)Proxy.newProxyInstance(ISetting.class.getClassLoader(),
                new Class[]{ISetting.class},settingHandler);
            P2PHandler.getInstance().p2pInit(this,p2pListener,settingListener);
            event("GWELL_P2P_LISTENERS_INITIALIZED: callbacks active");
        }catch(Throwable t){event("GWELL_LISTENER_INIT_ERROR: "+t.getClass().getSimpleName());}
    }
    private static Object defaultValue(Class<?> c){
        if(!c.isPrimitive()||c==Void.TYPE)return null;
        if(c==Boolean.TYPE)return false;
        if(c==Byte.TYPE)return (byte)0;
        if(c==Short.TYPE)return (short)0;
        if(c==Integer.TYPE)return 0;
        if(c==Long.TYPE)return 0L;
        if(c==Float.TYPE)return 0f;
        if(c==Double.TYPE)return 0d;
        if(c==Character.TYPE)return (char)0;
        return null;
    }
    private void connectP2p(int id,String user,long session1,long session2,int code1,int code2){
        if(id!=generation||closed)return;
        try{
            boolean ok=P2PHandler.getInstance().p2pConnect(user,(int)session1,
                (int)session2,code1,code2,0);
            p2pConnected=ok;
            event(ok?"GWELL_P2P_CONNECTED: SDK registration result true; camera session not yet proven":
                "GWELL_P2P_CONNECT_REJECTED: check vendor APPID/APPVERSION/session grant");
        }catch(Throwable t){
            event("GWELL_P2P_CONNECT_ERROR: "+t.getClass().getSimpleName());
        }
    }
    private void requestDeviceStatus(){
        if(!p2pConnected){event("GWELL_NO_P2P_REGISTRATION: perform vendor login and connect first");return;}
        try{
            versionKnown=false;
            P2PHandler.getInstance().getFriendStatus(new String[]{deviceId});
            event("GWELL_DEVICE_STATUS_REQUESTED: awaiting P2P version callback");
        }catch(Throwable t){event("GWELL_STATUS_REQUEST_FAILED: "+t.getClass().getSimpleName());}
    }
    private void startMonitor(){
        if(!p2pConnected){event("GWELL_NOT_CONNECTED: vendor P2P login needed");return;}
        if(!versionKnown){
            event("GWELL_VERSION_UNKNOWN: click 'Periksa P2P Device & Versi' and wait for callback");
            return;
        }
        if(pView==null){event("GWELL_VIEW_MISSING: GWell native view not initialized");return;}
        String monitorPassword=devicePasswordInput.getText().toString();
        if(monitorPassword.isEmpty()){
            event("GWELL_MONITOR_PASSWORD_REQUIRED: enter device monitoring password supplied by owner, no default guess");
            return;
        }
        try{
            String encoded=P2PHandler.getInstance().EntryPassword(monitorPassword);
            channelId=P2PHandler.getInstance().call(userId,encoded,true,1,deviceId,"","",2,deviceId,0);
            monitorAccepted=false;monitorReady=false;videoPtsSeen=false;
            event("GWELL_MONITOR_CALL_SENT: channel established? awaiting vAccept/vConnectReady and video PTS");
        }catch(Throwable t){event("GWELL_MONITOR_CALL_ERROR: "+t.getClass().getSimpleName());}
    }
    private void stopMonitor(){
        try{
            if(channelId>=0)P2PHandler.getInstance().finish(channelId);
            event("GWELL_MONITOR_STOP: video channel released");
        }catch(Throwable t){event("GWELL_MONITOR_STOP_ERROR: "+t.getClass().getSimpleName());}
    }
    private void copyReport(){
        String entries;
        synchronized(log){entries=log.toString();}
        String report="PBS Yoosee Native GWell P2P-Core · build v1.3.3"+
            "\nDevice ID: "+maskedDevice()+
            "\nlegacySdk: jwkj/GwellDemo P2P-Core 0.5.4 (archived 2019)"+
            "\nappVersionProvided: "+(!get(appVersionInput).isEmpty())+
            "\nvendorLoginAccepted: "+loginAccepted+
            "\np2pConnectResult: "+p2pConnected+
            "\nversionFromVendor: "+versionKnown+
            "\nmonitorAccepted: "+monitorAccepted+
            "\nmonitorReady: "+monitorReady+
            "\nvideoPtsSeen: "+videoPtsSeen+
            "\nlastOutcome: "+lastOutcome+
            "\nEVENTS:\n"+entries+
            "\nNo APPID, token, account name, UserID, SessionIDs, verification codes, device password or raw SDK data are exported.";
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
            ClipData.newPlainText("PBS GWell Native Diagnostic",report));
        event("GWELL_REPORT_COPIED: redacted report copied");
    }
    @Override public void onDestroy(){
        closed=true;generation++;
        NativeCrashLedger.exited(this);
        try{if(channelId>=0)P2PHandler.getInstance().finish(channelId);}catch(Throwable ignored){}
        try{if(p2pConnected)P2PHandler.getInstance().p2pDisconnect();}catch(Throwable ignored){}
        worker.shutdownNow();p2pListener=null;settingListener=null;
        super.onDestroy();
    }
    @Override protected void onCaptureScreenResult(boolean ok,int mode,boolean isCapture){}
    @Override protected void onP2PViewSingleTap(){}
    @Override protected void onP2PViewFilling(){}
    @Override protected void turnCamera(){}
    @Override protected void native_p2p_control(int value){}
    @Override protected void onVideoPTS(long pts){
        if(pts>0 && !videoPtsSeen){videoPtsSeen=true;event("GWELL_VIDEO_PTS_RECEIVED: native media timing present, verify image visually");}
    }
    @Override public int getActivityInfo(){return 0;}
    @Override protected void onGoBack(){}
    @Override protected void onGoFront(){}
    @Override protected void onExit(){}
}
