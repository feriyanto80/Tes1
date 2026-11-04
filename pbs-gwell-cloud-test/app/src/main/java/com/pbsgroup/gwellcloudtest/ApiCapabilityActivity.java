package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Read-only capability test for the currently logged-in user's own Yoosee/Gwell account.
 * No credential values, complete device identifiers, raw response bodies, or playback URLs
 * are written to the report. VAS POST calls are deliberately not sent without a supported
 * vendor-authorized signer and entitlement.
 */
public final class ApiCapabilityActivity extends Activity {
    private final int bg=Color.rgb(10,18,31),panel=Color.rgb(25,36,55),
            white=Color.WHITE,muted=Color.rgb(169,184,200),accent=Color.rgb(36,197,164);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private EditText deviceIdInput;
    private TextView statusView;
    private Button runButton;
    private String reportText="{\"status\":\"NOT_RUN\"}";
    private String initialDeviceId="";

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    private TextView label(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setPadding(0,dp(6),0,dp(6));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private Button button(LinearLayout root,String caption,boolean primary){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(primary?bg:white);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(shape(primary?accent:panel));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));
        p.topMargin=dp(9);root.addView(b,p);return b;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        initialDeviceId=getIntent().getStringExtra("device_id");
        if(initialDeviceId==null)initialDeviceId="";
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(28));scroll.addView(root);setContentView(scroll);
        root.addView(label("PBS GROUP · CLOUDLINKS DIAGNOSTICS",12,accent,true));
        root.addView(label("API Capability Test",26,white,true));
        root.addView(label("Pengujian read-only memakai sesi akun yang sedang login. Password, AppToken, AccessToken, signature, URL HLS, dan respons mentah tidak ditampilkan atau dikirim ke server PBS.",12,muted,false));

        LinearLayout sessionCard=new LinearLayout(this);sessionCard.setOrientation(LinearLayout.VERTICAL);
        sessionCard.setPadding(dp(13),dp(11),dp(13),dp(11));sessionCard.setBackground(shape(panel));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);root.addView(sessionCard,cp);
        sessionCard.addView(label("SESSION STATUS",12,accent,true));
        String sessionState=P2PSession.ready()?"LOGIN SESSION AVAILABLE":"LOGIN REQUIRED";
        sessionCard.addView(label(sessionState,15,white,true));
        sessionCard.addView(label("AppID tersedia: "+(!empty(P2PSession.appId()))+
                " · AppToken tersedia: "+(!empty(P2PSession.appToken())),11,muted,false));

        root.addView(label("Device ID untuk pemeriksaan detail (opsional)",12,muted,false));
        deviceIdInput=new EditText(this);deviceIdInput.setSingleLine(true);deviceIdInput.setText(initialDeviceId);
        deviceIdInput.setTextColor(white);deviceIdInput.setHintTextColor(muted);
        deviceIdInput.setHint("Masukkan Device ID milik akun Anda");
        deviceIdInput.setPadding(dp(12),0,dp(12),0);deviceIdInput.setBackground(shape(panel));
        root.addView(deviceIdInput,new LinearLayout.LayoutParams(-1,dp(48)));

        runButton=button(root,"▶ Jalankan API Capability Test",true);
        runButton.setOnClickListener(v->runTests());
        Button liveP2p=button(root,"▶ Buka Uji Live P2P Native",false);
        liveP2p.setOnClickListener(v->openNativeP2P());
        Button copy=button(root,"Salin laporan JSON (redacted)",false);
        copy.setOnClickListener(v->copyReport());
        Button back=button(root,"Kembali",false);back.setOnClickListener(v->finish());

        statusView=label("Belum ada pengujian. Tekan tombol untuk memulai.",13,white,false);
        statusView.setTextIsSelectable(true);statusView.setPadding(dp(13),dp(13),dp(13),dp(13));
        statusView.setBackground(shape(panel));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(14);root.addView(statusView,sp);

        root.addView(label("PENTING: AI Event dan Cloud Playback memerlukan autentikasi/signature VAS yang didukung vendor. Aplikasi hanya memeriksa konektivitas HTTPS host; tidak mengirim request VAS tanpa signer resmi. Respons HTTP dari HEAD bukan bukti hak akses API.",11,muted,false));
    }
    private boolean empty(String s){return s==null||s.trim().isEmpty();}
    private void openNativeP2P(){
        String id=deviceIdInput.getText().toString().trim();
        if(!P2PSession.ready()){
            status("LOGIN REQUIRED: login ke akun Yoosee/Gwell terlebih dahulu.");
            return;
        }
        if(empty(id)||!id.matches("[A-Za-z0-9_-]{1,100}")){
            status("Masukkan Device ID kamera milik akun Anda terlebih dahulu.");
            return;
        }
        Intent intent=new Intent(this,NativeP2PActivity.class);
        intent.putExtra("device_id",id);
        startActivity(intent);
    }
    private void runTests(){
        if(runButton!=null)runButton.setEnabled(false);
        status("Menjalankan pengujian. Proses berjalan di background; jangan tutup aplikasi.");
        final String requestedId=deviceIdInput.getText().toString().trim();
        worker.execute(()->{
            JSONObject report=new JSONObject();
            JSONArray tests=new JSONArray();
            String selectedId=requestedId;
            try{
                report.put("application","PBS Yoosee API Capability Test");
                report.put("version","1.2.6");
                report.put("generatedAtUtc",utcNow());
                report.put("testMode","READ_ONLY");
                report.put("credentialsIncluded",false);
                report.put("rawResponseIncluded",false);
                report.put("sessionPresent",P2PSession.ready());
                report.put("appIdPresent",!empty(P2PSession.appId()));
                report.put("appTokenPresent",!empty(P2PSession.appToken()));
                report.put("deviceId",maskId(requestedId));

                if(P2PSession.ready()&&!empty(P2PSession.appId())&&!empty(P2PSession.appToken())){
                    YooseeApi api=new YooseeApi(P2PSession.appId(),P2PSession.appToken());
                    api.restoreSession(P2PSession.id(),P2PSession.token());

                    long start=System.currentTimeMillis();
                    try{
                        JSONObject response=api.listDevices();
                        long elapsed=System.currentTimeMillis()-start;
                        int code=response.optInt("code",-1);
                        JSONArray list=extractDeviceList(response);
                        int count=list==null?0:list.length();
                        JSONObject first=list==null||list.length()==0?null:list.optJSONObject(0);
                        String firstKeys=first==null?"none":keys(first);
                        add(tests,"DEVICE_LIST","0".equals(String.valueOf(code))||code==0?"PASS":"API_REJECTED",
                                "HTTP "+response.optInt("_httpStatus",-1)+"; API code "+code+
                                "; deviceCount "+count+"; firstDeviceFields "+firstKeys+
                                "; deviceStatusSummary="+summarizeDeviceStatuses(list)+
                                "; latencyMs "+elapsed,elapsed);
                        if(empty(selectedId)&&first!=null){
                            selectedId=firstId(first);
                            final String autoSelectedId=selectedId;
                            runOnUiThread(()->{if(deviceIdInput!=null&&empty(deviceIdInput.getText().toString()))deviceIdInput.setText(autoSelectedId);});
                        }
                    }catch(Exception e){
                        add(tests,"DEVICE_LIST","ERROR",safeClass(e),System.currentTimeMillis()-start);
                    }

                    if(!empty(selectedId)&&selectedId.matches("[A-Za-z0-9_-]{1,100}")){
                        boolean infoQueryPassed=false;
                        long infoStart=System.currentTimeMillis();
                        try{
                            JSONObject response=api.deviceInfo(selectedId);
                            long elapsed=System.currentTimeMillis()-infoStart;
                            int code=response.optInt("code",-1);
                            JSONObject data=response.optJSONObject("data");
                            String fields=data==null?"none":keys(data);
                            String statusField=extractStatusField(data);
                            infoQueryPassed=(code==0);
                            add(tests,"DEVICE_INFO_GET",infoQueryPassed?"PASS":"API_REJECTED",
                                    "GET /openapi/app/user/device/getDeviceInfo; HTTP "+response.optInt("_httpStatus",-1)+
                                    "; API code "+code+"; statusField="+statusField+
                                    "; dataKeys="+fields+"; deviceId="+maskId(selectedId)+
                                    "; latencyMs="+elapsed,elapsed);
                            if(infoQueryPassed){
                                add(tests,"DEVICE_STATUS_DETAIL",
                                        "not_returned".equals(statusField)?"NOT_RETURNED":"FIELD_PRESENT_UNVERIFIED",
                                        "Status field dari getDeviceInfo: "+statusField+
                                        ". Nilai enum numerik tidak diterjemahkan menjadi online/offline tanpa dokumentasi vendor.",elapsed);
                            }
                        }catch(Exception e){
                            add(tests,"DEVICE_INFO_GET","ERROR",safeClass(e),System.currentTimeMillis()-infoStart);
                        }

                        if(!infoQueryPassed){
                            for(String parameter:new String[]{"devId","deviceId"}){
                                if(infoQueryPassed)break;
                                start=System.currentTimeMillis();
                                try{
                                    JSONObject response=api.deviceInfoQuery(selectedId,parameter);
                                    long elapsed=System.currentTimeMillis()-start;
                                    int code=response.optInt("code",-1);
                                    JSONObject data=response.optJSONObject("data");
                                    String fields=data==null?"none":keys(data);
                                    String msg=safeMessage(response.optString("msg",""));
                                    String statusField=extractStatusField(data);
                                    infoQueryPassed=(code==0);
                                    add(tests,"DEVICE_INFO_QUERY_"+parameter.toUpperCase(Locale.ROOT),
                                            infoQueryPassed?"PASS":"API_REJECTED",
                                            "GET /openapi/app/user/device/infoQuery; parameter="+parameter+
                                            "; HTTP "+response.optInt("_httpStatus",-1)+"; API code "+code+
                                            "; msg="+msg+"; statusField="+statusField+"; dataKeys="+fields+
                                            "; deviceId="+maskId(selectedId)+"; latencyMs="+elapsed,elapsed);
                                    if(infoQueryPassed){
                                        add(tests,"DEVICE_STATUS_DETAIL",
                                                "not_returned".equals(statusField)?"NOT_RETURNED":"FIELD_PRESENT_UNVERIFIED",
                                                "Status field dari infoQuery: "+statusField+
                                                ". Nilai enum numerik tidak diterjemahkan tanpa dokumentasi vendor.",elapsed);
                                    }
                                }catch(Exception e){
                                    add(tests,"DEVICE_INFO_QUERY_"+parameter.toUpperCase(Locale.ROOT),
                                            "ERROR",safeClass(e),System.currentTimeMillis()-start);
                                }
                            }
                        }else{
                            add(tests,"DEVICE_INFO_QUERY_FALLBACK","SKIPPED",
                                    "Fallback infoQuery tidak diperlukan karena getDeviceInfo berhasil.",0);
                        }
                    }else{
                        add(tests,"DEVICE_INFO_GET","SKIPPED","Device ID tidak diberikan dan tidak dapat diambil dari respons list.",0);
                        add(tests,"DEVICE_INFO_QUERY","SKIPPED","Tidak ada Device ID untuk menguji fallback infoQuery.",0);
                    }

                    api.close();
                }else{
                    add(tests,"SESSION","BLOCKED","Login terlebih dahulu melalui layar utama; AppID/AppToken harus diisi oleh pemilik aplikasi.",0);
                    add(tests,"DEVICE_LIST","SKIPPED","Tidak ada sesi akun yang tersedia di memori.",0);
                    add(tests,"DEVICE_INFO_QUERY","SKIPPED","Tidak ada sesi akun atau Device ID yang dapat diuji.",0);
                }

                probe(tests,"OPENAPI_BASE","openapi-iot.cloudlinks.cn","/openapi/app");
                probe(tests,"NETWORK_CONFIG_HOST","openapi-iot.cloudlinks.cn","/openapi/netcfg/app/getConnectInfo");
                probe(tests,"AI_EVENT_HOST","saas-playback.cloudlinks.cn","/openapi/vpaasPlayback/vas/aievent/list");
                probe(tests,"CLOUD_PLAYBACK_HOST","saas-playback.cloudlinks.cn","/openapi/vpaasPlayback/vas/playback/play");

                add(tests,"LIVE_P2P_SDK_TEST",P2PSession.ready()&&!empty(selectedId)?"MANUAL_TEST_REQUIRED":"BLOCKED",
                        "Live P2P tidak diuji otomatis oleh API scanner. Gunakan tombol Buka Uji Live P2P Native; bukti stream adalah callback STATE_PLAY disertai dimensi video > 0 dan laju data. REGISTER_CALL_RETURNED saja bukan bukti autentikasi.",0);
                add(tests,"AI_EVENT_AUTH","NOT_TESTED_VENDOR_SIGNER_REQUIRED",
                        "Tidak mengirim request API. Endpoint v2 pada referensi publik memerlukan signature runtime sha1WithBase256; metode signing resmi/otorisasi VAS belum tersedia di konfigurasi PBS saat ini.",0);
                add(tests,"CLOUD_PLAYBACK_AUTH","NOT_TESTED_VENDOR_SIGNER_REQUIRED",
                        "Tidak meminta URL HLS dan tidak mengunduh rekaman. Memerlukan autentikasi VAS dan hak akses playback yang dikonfirmasi Gwell.",0);

                report.put("selectedDeviceId",maskId(selectedId));
                report.put("tests",tests);
                int pass=0,failed=0,blocked=0,notTested=0;
                for(int i=0;i<tests.length();i++){
                    String st=tests.getJSONObject(i).optString("status");
                    if("PASS".equals(st))pass++;
                    else if("ERROR".equals(st)||"API_REJECTED".equals(st))failed++;
                    else if("BLOCKED".equals(st)||"MANUAL_TEST_REQUIRED".equals(st))blocked++;
                    else if(st.startsWith("NOT_TESTED")||"SKIPPED".equals(st)||"NOT_RETURNED".equals(st)||"FIELD_PRESENT_UNVERIFIED".equals(st))notTested++;
                }
                JSONObject summary=new JSONObject();
                summary.put("passed",pass);summary.put("failedOrRejected",failed);
                summary.put("blockedOrNotTested",blocked+notTested);summary.put("total",tests.length());
                report.put("summary",summary);
                report.put("overall","COMPLETED_WITH_LIMITATIONS");
            }catch(Exception fatal){
                try{
                    report.put("overall","TEST_ERROR");
                    report.put("fatalError",safeClass(fatal));
                    report.put("tests",tests);
                }catch(Exception ignored){}
            }
            reportText=report.toString();
            final String display=makeSummary(report);
            runOnUiThread(()->{
                statusView.setText(display+"\n\nJSON REPORT\n"+reportText);
                if(runButton!=null)runButton.setEnabled(true);
            });
        });
    }
    private JSONArray extractDeviceList(JSONObject response){
        JSONObject data=response.optJSONObject("data");
        if(data!=null){
            JSONArray a=data.optJSONArray("deviceList");if(a!=null)return a;
            a=data.optJSONArray("list");if(a!=null)return a;
            a=data.optJSONArray("devices");if(a!=null)return a;
        }
        return response.optJSONArray("data");
    }
    private String extractStatusField(JSONObject data){
        if(data==null)return "not_returned";
        for(String key:new String[]{"online","isOnline","onlineStatus","deviceStatus","status","statusCode","linkStatus"}){
            if(data.has(key)&&!data.isNull(key)){
                Object value=data.opt(key);
                return key+"="+safeMessage(String.valueOf(value));
            }
        }
        return "not_returned";
    }
    private String summarizeDeviceStatuses(JSONArray list){
        if(list==null)return "deviceList_not_returned";
        int withField=0,explicitOnline=0,explicitOffline=0,unverified=0,missing=0;
        StringBuilder fields=new StringBuilder();
        for(int i=0;i<list.length();i++){
            JSONObject row=list.optJSONObject(i);
            if(row==null){missing++;continue;}
            String found="";Object value=null;
            for(String key:new String[]{"online","isOnline","onlineStatus","deviceStatus","status","statusCode","linkStatus"}){
                if(row.has(key)&&!row.isNull(key)){found=key;value=row.opt(key);break;}
            }
            if(found.isEmpty()){missing++;continue;}
            withField++;
            if(fields.indexOf(found)<0){if(fields.length()>0)fields.append(",");fields.append(found);}
            if(value instanceof Boolean){
                if(((Boolean)value).booleanValue())explicitOnline++;else explicitOffline++;
            }else{
                String v=String.valueOf(value).trim().toLowerCase(Locale.ROOT);
                if("online".equals(v)||"true".equals(v))explicitOnline++;
                else if("offline".equals(v)||"false".equals(v))explicitOffline++;
                else unverified++;
            }
        }
        return "fields="+(fields.length()==0?"none":fields.toString())+
                "; rowsWithStatus="+withField+"/"+list.length()+
                "; explicitOnline="+explicitOnline+"; explicitOffline="+explicitOffline+
                "; unverifiedVendorValues="+unverified+"; missing="+missing+
                "; numeric/vendor enums are not interpreted";
    }
    private String keys(JSONObject object){
        JSONArray names=object.names();
        return names==null?"none":names.toString();
    }
    private String safeMessage(String message){
        if(empty(message))return "not_returned";
        String clean=message.replaceAll("[\\r\\n\\t]"," ").replaceAll("[^A-Za-z0-9 _().,:/-]","");
        return clean.length()>120?clean.substring(0,120):clean;
    }
    private String firstId(JSONObject row){
        for(String k:new String[]{"deviceId","devId","deviceID","device_id","id"}){
            String v=row.optString(k,"");if(!empty(v)&&!"null".equalsIgnoreCase(v))return v;
        }
        return "";
    }
    private void probe(JSONArray tests,String name,String host,String path){
        long start=System.currentTimeMillis();
        HttpsURLConnection c=null;
        try{
            URL url=new URL("https://"+host+path);
            c=(HttpsURLConnection)url.openConnection();
            c.setRequestMethod("HEAD");c.setConnectTimeout(8000);c.setReadTimeout(8000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","PBS-Yoosee-Capability-Test/1.2.6");
            c.setRequestProperty("Accept","application/json");
            int http=c.getResponseCode();
            long elapsed=System.currentTimeMillis()-start;
            String probeStatus=http>=200&&http<300?"HTTP_REACHABLE":(http==401||http==403?"ACCESS_RESTRICTED":(http==404?"PATH_NOT_FOUND":(http>=500?"SERVER_ERROR":"HTTP_RESPONSE_OTHER")));
            add(tests,name,probeStatus,
                    "TLS/HTTP response received; HTTP "+http+
                    "; endpoint path "+path+
                    "; this HEAD result does not prove authenticated API entitlement.",elapsed);
        }catch(Exception e){
            add(tests,name,"NETWORK_ERROR",safeClass(e),System.currentTimeMillis()-start);
        }finally{if(c!=null)c.disconnect();}
    }
    private void add(JSONArray arr,String name,String status,String details,long elapsed){
        JSONObject item=new JSONObject();
        try{
            item.put("test",name);item.put("status",status);
            item.put("details",details);item.put("latencyMs",elapsed);arr.put(item);
        }catch(Exception ignored){}
    }
    private String maskId(String id){
        if(empty(id))return "not_selected";
        if(id.length()<=4)return "****";
        return "****"+id.substring(id.length()-4);
    }
    private String safeClass(Throwable e){
        return e.getClass().getSimpleName()+" (detail omitted to avoid exposing credentials or server data)";
    }
    private String utcNow(){
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());
    }
    private String makeSummary(JSONObject report){
        JSONObject s=report.optJSONObject("summary");
        if(s==null)return "Pengujian selesai, tetapi laporan ringkas tidak tersedia.";
        return "API CAPABILITY TEST SELESAI\n"+
                "Pass: "+s.optInt("passed")+" · Rejected/Error: "+s.optInt("failedOrRejected")+
                " · Blocked/Not tested: "+s.optInt("blockedOrNotTested")+
                "\nStatus: "+report.optString("overall");
    }
    private void status(String value){
        runOnUiThread(()->{if(statusView!=null)statusView.setText(value);});
    }
    private void copyReport(){
        ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("PBS API Capability Report",reportText));
        status("Laporan JSON sudah disalin. Periksa sebelum dibagikan; laporan menyamarkan Device ID dan tidak menyertakan token.");
    }
    @Override protected void onDestroy(){
        worker.shutdownNow();super.onDestroy();
    }
}
