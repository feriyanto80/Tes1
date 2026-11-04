package com.pbsgroup.gwellcloudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Safe endpoint inventory scanner.
 * Sends only OPTIONS and (for read-only candidates) HEAD requests.
 * Never sends credentials, request bodies, passwords, device IDs or mutation requests.
 */
public final class EndpointInventoryActivity extends Activity {
    private final int bg=Color.rgb(10,18,31),panel=Color.rgb(25,36,55),
            white=Color.WHITE,muted=Color.rgb(169,184,200),accent=Color.rgb(36,197,164);
    private final ExecutorService scanWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService networkPool=Executors.newFixedThreadPool(8);
    private Button runButton;
    private TextView statusView;
    private String reportText="{\"status\":\"NOT_RUN\"}";

    private static final String[] LEGACY_HOSTS={"api1.cloudlinks.cn","api2.cloudlinks.cn"};
    private static final String[] OPENAPI_HOST={"openapi-iot.cloudlinks.cn"};
    private static final String[] PLAYBACK_HOST={"saas-playback.cloudlinks.cn"};
    private static final String[] VAS_CANDIDATE_HOST={"vasapi.cloudlinks.cn:10443"};

    private static final class Endpoint {
        final String group,path,risk,hostConfidence;
        final String[] hosts;
        final boolean headSafe;
        Endpoint(String group,String path,String[] hosts,boolean headSafe,String risk,String hostConfidence){
            this.group=group;this.path=path;this.hosts=hosts;this.headSafe=headSafe;
            this.risk=risk;this.hostConfidence=hostConfidence;
        }
    }

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
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(28));scroll.addView(root);setContentView(scroll);

        root.addView(label("PBS GROUP · Gwell / Yoosee",12,accent,true));
        root.addView(label("Endpoint Inventory Scan",25,white,true));
        root.addView(label("Memeriksa daftar endpoint legacy .ashx, OpenAPI, VAS, AI Event, Playback, Network Config, dan Auth.",13,muted,false));

        LinearLayout warning=new LinearLayout(this);warning.setOrientation(LinearLayout.VERTICAL);
        warning.setPadding(dp(13),dp(12),dp(13),dp(12));warning.setBackground(shape(panel));
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,-2);wp.topMargin=dp(10);root.addView(warning,wp);
        warning.addView(label("MODE AMAN · TANPA LOGIN REQUEST",12,accent,true));
        warning.addView(label("Scanner hanya mengirim HTTP OPTIONS dan HEAD. Tidak mengirim password, token, body, Device ID, atau menjalankan Login/Logout, Bind/Unbind, Modify, Delete, Firmware Upgrade, SMS/Email, playback, maupun operasi lain yang dapat mengubah data.",12,white,false));
        warning.addView(label("HTTP 200 atau 405 hanya membuktikan server memberi respons. Ini BUKAN bukti autentikasi, izin AppID, hak VAS, ataupun endpoint dapat dipakai dengan GET/POST.",11,muted,false));

        runButton=button(root,"▶ Cek seluruh endpoint (OPTIONS / HEAD)",true);
        runButton.setOnClickListener(v->runScan());
        Button copy=button(root,"Salin laporan JSON (redacted)",false);
        copy.setOnClickListener(v->copyReport());
        Button back=button(root,"Kembali",false);back.setOnClickListener(v->finish());

        statusView=label("Belum ada pemindaian. Tekan tombol untuk mulai.",13,white,false);
        statusView.setTextIsSelectable(true);statusView.setPadding(dp(13),dp(13),dp(13),dp(13));
        statusView.setBackground(shape(panel));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(14);root.addView(statusView,sp);
    }

    private void add(List<Endpoint> out,String group,String path,String[] hosts,boolean headSafe,String risk,String confidence){
        out.add(new Endpoint(group,path,hosts,headSafe,risk,confidence));
    }
    private void addLegacy(List<Endpoint> out,String group,String[] paths,boolean headSafe,String risk){
        for(String path:paths)add(out,group,path,LEGACY_HOSTS,headSafe,risk,"KNOWN_CANDIDATE");
    }
    private void addOpenApi(List<Endpoint> out,String group,String prefix,String[] suffixes,boolean headSafe,String risk){
        for(String suffix:suffixes)add(out,group,prefix+suffix,OPENAPI_HOST,headSafe,risk,"KNOWN_CANDIDATE");
    }
    private void addPlayback(List<Endpoint> out,String group,String prefix,String[] suffixes,boolean headSafe,String risk){
        for(String suffix:suffixes)add(out,group,prefix+suffix,PLAYBACK_HOST,headSafe,risk,"KNOWN_CANDIDATE");
    }

    private List<Endpoint> catalog(){
        List<Endpoint> out=new ArrayList<>();

        addLegacy(out,"Legacy · Users",new String[]{
            "Users/LoginCheck.ashx","Users/RegisterCheck.ashx","Users/Logout.ashx",
            "Users/InitPassword.ashx","Users/ModifyPwd.ashx","Users/ModifyNickname.ashx",
            "Users/SendMail.ashx","Users/MailCodeCheck.ashx","Users/PhoneCheckCode.ashx",
            "Users/PhoneVerifyCodeCheck.ashx","Users/ThirdLogin.ashx","Users/UpdateSafeSet.ashx",
            "Users/UploadPushParam.ashx"
        },false,"AUTH_OR_MUTATION");
        addLegacy(out,"Legacy · Users",new String[]{"Users/GetCountryCodeList.ashx"},true,"READ_ONLY_CANDIDATE");

        addLegacy(out,"Legacy · Password",new String[]{
            "Password/ResetPWD.ashx","Password/GetAccountByEmail.ashx",
            "Password/GetAccountByPhoneNO.ashx","Password/CheckPhoneVKey.ashx"
        },false,"CREDENTIAL_OR_PRIVACY_SENSITIVE");

        addLegacy(out,"Legacy · Account",new String[]{"Account/IsAccountExisted.ashx"},false,"ACCOUNT_ENUMERATION_SENSITIVE");
        addLegacy(out,"Legacy · Account Bind",new String[]{
            "Account/Bind/BindAccountEx.ashx","Account/Bind/RemoveBindEx.ashx",
            "Account/Bind/SearchBindAccountEx.ashx","Account/Bind/SearchBindDev.ashx",
            "Account/Bind/ModifyNickname.ashx"
        },false,"ACCOUNT_BIND_OR_PRIVACY_SENSITIVE");

        addLegacy(out,"Legacy · Relationship",new String[]{
            "Relationship/AddDevice.ashx","Relationship/BindOwner.ashx",
            "Relationship/DeleteDevice.ashx","Relationship/ModifyDevice.ashx",
            "Relationship/DeleteGuestInfo.ashx","Relationship/DeleteOwnerInfo.ashx",
            "Relationship/ModifyPermission.ashx","Relationship/ModifyGuestRemarkname.ashx",
            "Relationship/UpgradeDeviceInfo.ashx","Relationship/SetDevOffinePush.ashx",
            "Relationship/CancelReceivePush.ashx","Relationship/LockDeviceBindOwner.ashx",
            "Relationship/CancelShare.ashx","Relationship/GetDevPwd.ashx",
            "Relationship/OpenAuthority.ashx","Relationship/SwitchOwner.ashx",
            "Relationship/cacheNetwork.ashx"
        },false,"DEVICE_MUTATION_OR_SECRET");
        addLegacy(out,"Legacy · Relationship",new String[]{
            "Relationship/GetGuestInfo.ashx","Relationship/GetGuestList.ashx",
            "Relationship/GetSharedDevInfo.ashx","Relationship/LoadDeviceInfo.ashx",
            "Relationship/GetShareLink.ashx","Relationship/V2/GetBindedStatus.ashx"
        },true,"READ_ONLY_CANDIDATE");

        addLegacy(out,"Legacy · Alarm / AppInfo",new String[]{
            "Alarm/AlarmRecordEx.ashx","AppInfo/getappstartinfo.ashx",
            "AppInfo/getstorelinks.ashx","appinfo/BootDiagram/IsShow.ashx"
        },true,"READ_ONLY_CANDIDATE");
        addLegacy(out,"Legacy · AppInfo",new String[]{"AppInfo/SetStoreID.ashx"},false,"STATE_MUTATION");
        addLegacy(out,"Legacy · Firmware / Position / Business",new String[]{
            "api/Device/GetUpgInfo.ashx","api/app/checkupgrade.ashx",
            "api/deviceinfo/position.ashx","business/seller/recommendinfo.ashx"
        },true,"READ_ONLY_CANDIDATE");

        addOpenApi(out,"OpenAPI · User","/openapi/app/user/",new String[]{
            "login/account","login/third","login/loginScanQrcode","login/thirdIsExist",
            "regist","emailRegist","logout","unregist","modifyPwd","forgotPwd","resetPwd",
            "sendSmsCode","sendEmailCode","verifyCode","sendSmsCodeSecure",
            "modifyInfo","accountBind","thirdBind","thirdUnbind","thirdRegist",
            "regionConfirm","reGenUsrAcceccToken","pushTokenBind","uploadLog",
            "checkEmailExist","checkMobileExist","getCosCredential","cos/getAuthInfo"
        },false,"AUTH_ACCOUNT_OR_PRIVACY_SENSITIVE");
        addOpenApi(out,"OpenAPI · User Read Candidates","/openapi/app/user/",new String[]{
            "queryInfo","getConfig"
        },true,"READ_ONLY_CANDIDATE");

        addOpenApi(out,"OpenAPI · Device","/openapi/app/user/device/",new String[]{
            "bind","unbind","modifyDevInfo","modifyRemarkName","shareGuest","deleteShare",
            "confirmShare","modifyGuestRemark","modifyGuestPermission","getQrcodeToken",
            "scanShareQrcode","submitQut","preset/addPosition","preset/deletePosition",
            "preset/modifyPosition","preset/swapPosition"
        },false,"DEVICE_MUTATION_OR_TOKEN");
        addOpenApi(out,"OpenAPI · Device Read Candidates","/openapi/app/user/device/",new String[]{
            "listDevice","infoQuery","queryDevOwner","listGuest","inviteInfo",
            "queryGuestInfo","getIntercomConfig","history/list","preset/listPosition"
        },true,"READ_ONLY_CANDIDATE");

        addOpenApi(out,"OpenAPI · Firmware","/openapi/app/",new String[]{"upg/queryVersion"},true,"READ_ONLY_CANDIDATE");
        addOpenApi(out,"OpenAPI · Firmware","/openapi/app/user/",new String[]{"upgrade"},false,"FIRMWARE_MUTATION");

        addOpenApi(out,"OpenAPI · VAS Catalog","/openapi/app/vas/",new String[]{
            "package/list","package/listHotValue","service/outline","service/list"
        },true,"READ_ONLY_CANDIDATE");
        addOpenApi(out,"OpenAPI · VAS Orders","/openapi/app/vas/",new String[]{
            "order/generate","payment/generate"
        },false,"BILLING_MUTATION");
        addOpenApi(out,"OpenAPI · VAS Orders","/openapi/app/vas/",new String[]{
            "order/list","order/overview","payment/result"
        },false,"BILLING_OR_ACCOUNT_SENSITIVE");

        addOpenApi(out,"OpenAPI · Cloud Storage","/openapi/app/",new String[]{
            "cloudstoage/list"
        },true,"READ_ONLY_CANDIDATE");
        addOpenApi(out,"OpenAPI · Cloud Storage","/openapi/app/",new String[]{
            "cloudstoage/download","cloudstoage/speedPlay","cloudstorage/playback"
        },false,"RECORDING_ACCESS_OR_PLAYBACK");

        addPlayback(out,"OpenAPI · Cloud Playback","/openapi/vpaasPlayback/vas/playback/",new String[]{
            "list","listv2","datelist"
        },true,"READ_ONLY_CANDIDATE");
        addPlayback(out,"OpenAPI · Cloud Playback","/openapi/vpaasPlayback/vas/playback/",new String[]{
            "play","playv2","remove","removeall"
        },false,"PLAYBACK_OR_DELETE_OPERATION");
        addPlayback(out,"OpenAPI · Cloud Playback","/openapi/vpaasPlayback/vas/speedplayback/",new String[]{
            "play"
        },false,"PLAYBACK_OPERATION");

        addPlayback(out,"OpenAPI · AI Event","/openapi/vpaasPlayback/vas/aievent/",new String[]{
            "list","query","search","searchText","completelist"
        },true,"READ_ONLY_CANDIDATE");
        addPlayback(out,"OpenAPI · AI Event","/openapi/vpaasPlayback/vas/aievent/",new String[]{
            "remove"
        },false,"EVENT_DELETE_OPERATION");
        addPlayback(out,"OpenAPI · AI Face","/openapi/vpaasPlayback/vas/aiface/",new String[]{
            "list"
        },true,"READ_ONLY_CANDIDATE");
        addPlayback(out,"OpenAPI · AI Face","/openapi/vpaasPlayback/vas/aiface/",new String[]{
            "edit","merge","remove"
        },false,"FACE_DATA_MUTATION");

        addOpenApi(out,"OpenAPI · Network Config","/openapi/netcfg/app/",new String[]{
            "getConnectInfo","getDevInfoBySN","getDeviceBarcode","getGiveInfo",
            "getLinkInfo","appearanceInfo"
        },true,"READ_ONLY_CANDIDATE");
        addOpenApi(out,"OpenAPI · Network Config","/openapi/netcfg/app/ble/",new String[]{
            "getTanKey"
        },false,"CRYPTOGRAPHIC_SECRET");
        addOpenApi(out,"OpenAPI · Network Config","/openapi/netcfg/cloud/netcfg/",new String[]{
            "genbindtoken"
        },false,"BIND_TOKEN_GENERATION");

        addOpenApi(out,"OpenAPI · Advertising","/openapi/ad/",new String[]{"v3","v4","v5"},true,"READ_ONLY_CANDIDATE");

        add(out,"VAS API · Japan","/vas/ja/v1/vas/package/permission",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v1/vas/video/eventList",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v1/vas/service/getExpiringServiceDetail",VAS_CANDIDATE_HOST,false,"ACCOUNT_BILLING_SENSITIVE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v2/vas/app/devconfig/query",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v2/vas/app/event/delete",VAS_CANDIDATE_HOST,false,"EVENT_DELETE_OPERATION","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v2/vas/app/event/play",VAS_CANDIDATE_HOST,false,"PLAYBACK_OPERATION","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v2/vas/banner/getBannerList",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v2/vas/ai/getDeviceAISupportItem",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Japan","/vas/ja/v2/vas/coupon/receiveFirstBindCoupon",VAS_CANDIDATE_HOST,false,"COUPON_OR_ACCOUNT_MUTATION","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Playback","/vas/event/completelistv2",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");
        add(out,"VAS API · Playback","/vas/playback/list",VAS_CANDIDATE_HOST,true,"READ_ONLY_CANDIDATE","UNCONFIRMED_HOST_CANDIDATE");

        add(out,"Auth · Host Mapping Required","/v2/app/auth/mobile-login",new String[0],false,"AUTH_OPERATION","HOST_NOT_CONFIRMED");
        add(out,"Auth · Host Mapping Required","/v2/auth/refresh",new String[0],false,"AUTH_OPERATION","HOST_NOT_CONFIRMED");
        add(out,"Auth · Host Mapping Required","/v2/verify/send",new String[0],false,"SMS_OR_VERIFICATION_OPERATION","HOST_NOT_CONFIRMED");

        return out;
    }

    private void runScan(){
        runButton.setEnabled(false);
        status("Menyusun katalog dan memeriksa route. Scanner memakai OPTIONS; HEAD hanya untuk kandidat read-only. Mohon tunggu.");
        scanWorker.execute(()->{
            List<Endpoint> endpoints=catalog();
            JSONArray dnsDiagnostics=hostDiagnostics();
            List<Callable<JSONObject>> tasks=new ArrayList<>();
            for(Endpoint endpoint:endpoints)tasks.add(()->probeEndpoint(endpoint));
            JSONArray results=new JSONArray();
            try{
                List<Future<JSONObject>> futures=networkPool.invokeAll(tasks,90,TimeUnit.SECONDS);
                for(int i=0;i<futures.size();i++){
                    Future<JSONObject> future=futures.get(i);
                    if(future.isCancelled()){
                        results.put(timeoutResult(endpoints.get(i)));
                    }else{
                        try{results.put(future.get());}
                        catch(Exception e){results.put(errorResult(endpoints.get(i),e));}
                    }
                }
            }catch(Exception e){
                for(Endpoint endpoint:endpoints)results.put(errorResult(endpoint,e));
            }

            JSONObject report=new JSONObject();
            try{
                report.put("application","PBS Yoosee Endpoint Inventory Scanner");
                report.put("version","1.3.3");
                report.put("generatedAtUtc",utcNow());
                report.put("testMode","READ_ONLY_OPTIONS_AND_HEAD");
                report.put("credentialsIncluded",false);
                report.put("requestBodySent",false);
                report.put("authenticatedRequestsSent",false);
                report.put("mutationRequestsSent",false);
                report.put("endpointCount",endpoints.size());
                report.put("dnsDiagnostics",dnsDiagnostics);
                report.put("results",results);
                report.put("summary",summarize(results));
                report.put("interpretation","OPTIONS/HEAD response indicates transport or route-level response only. DNS failure means the candidate hostname did not resolve from this network; it does not prove the API is discontinued. HTTP 404 from OPTIONS/HEAD does not prove GET/POST route absence. No business authentication or entitlement is inferred.");
                report.put("hostNotes","Legacy api1/api2 are unconfirmed candidate hosts and are checked with DNS diagnostics. OpenAPI and playback hosts follow observed application/API responses. vasapi.cloudlinks.cn:10443 remains an unconfirmed candidate. /v2/auth host is not mapped and is not probed.");
                report.put("safetyNotes","State-changing, credential, firmware, sharing, delete, billing, playback, secret and token-generation endpoints receive OPTIONS only or are marked not probed when host is unknown. No GET/POST/PATCH/DELETE request is sent.");
                report.put("overall","COMPLETED_WITH_LIMITATIONS");
            }catch(Exception ignored){}
            reportText=report.toString();
            String display=makeSummary(report);
            runOnUiThread(()->{
                statusView.setText(display+"\n\nJSON REPORT\n"+reportText);
                runButton.setEnabled(true);
            });
        });
    }

    private JSONArray hostDiagnostics(){
        JSONArray out=new JSONArray();
        String[] hosts={"api1.cloudlinks.cn","api2.cloudlinks.cn","openapi-iot.cloudlinks.cn",
                "saas-playback.cloudlinks.cn","vasapi.cloudlinks.cn"};
        for(String host:hosts){
            long start=System.currentTimeMillis();
            JSONObject item=new JSONObject();
            try{
                InetAddress[] addresses=InetAddress.getAllByName(host);
                item.put("host",host);
                item.put("status",addresses!=null&&addresses.length>0?"DNS_RESOLVED":"DNS_NO_ADDRESS");
                item.put("resolvedAddressCount",addresses==null?0:addresses.length);
                item.put("latencyMs",System.currentTimeMillis()-start);
                item.put("resolvedAddressesIncluded",false);
            }catch(Exception e){
                try{
                    item.put("host",host);
                    item.put("status",e instanceof UnknownHostException?"DNS_LOOKUP_FAILED":"DNS_ERROR");
                    item.put("errorClass",e.getClass().getSimpleName());
                    item.put("resolvedAddressCount",0);
                    item.put("latencyMs",System.currentTimeMillis()-start);
                    item.put("resolvedAddressesIncluded",false);
                }catch(Exception ignored){}
            }
            out.put(item);
        }
        return out;
    }

    private JSONObject probeEndpoint(Endpoint e){
        JSONObject item=new JSONObject();
        JSONArray attempts=new JSONArray();
        String bestStatus=e.hosts.length==0?"HOST_MAPPING_REQUIRED":"NO_RESPONSE";
        String bestHost="";
        int bestPriority=-10;
        try{
            item.put("group",e.group);item.put("path",e.path);item.put("riskClass",e.risk);
            item.put("hostConfidence",e.hostConfidence);
            item.put("probeMethods",e.headSafe?"OPTIONS,HEAD":"OPTIONS_ONLY");
            item.put("requestBodySent",false);item.put("credentialsSent",false);
            if(e.hosts.length==0){
                item.put("status","HOST_MAPPING_REQUIRED");
                item.put("details","Endpoint dicatat, tetapi base host belum dapat dipastikan. Tidak ada request jaringan yang dikirim.");
                item.put("attemptCount",0);item.put("attempts",attempts);
                return item;
            }
            for(String host:e.hosts){
                JSONObject options=probeOne("OPTIONS",host,e.path);
                attempts.put(options);
                String optionsStatus=options.optString("status");
                int p=priority(optionsStatus);
                if(p>bestPriority){
                    bestPriority=p;bestStatus=optionsStatus;
                    bestHost=isHttpResponseStatus(optionsStatus)?host:"";
                }
                boolean retryHost=isRetryable(optionsStatus);
                if(e.headSafe){
                    JSONObject head=probeOne("HEAD",host,e.path);
                    attempts.put(head);
                    String headStatus=head.optString("status");
                    p=priority(headStatus);
                    if(p>bestPriority){
                        bestPriority=p;bestStatus=headStatus;
                        bestHost=isHttpResponseStatus(headStatus)?host:"";
                    }
                    retryHost=retryHost||isRetryable(headStatus);
                    if(isPositiveResponse(optionsStatus)||isPositiveResponse(headStatus))break;
                }
                if(!retryHost)break;
            }
            item.put("status",bestStatus);
            item.put("respondingHost",bestHost);
            item.put("attemptCount",attempts.length());
            item.put("attempts",attempts);
            item.put("details","Route-level probe only. HTTP response does not prove API authentication or entitlement. DNS/network failures are reported separately and do not prove the route is discontinued.");
        }catch(Exception ex){
            try{
                item.put("status","SCAN_ERROR");item.put("details",safeClass(ex));
                item.put("attemptCount",attempts.length());item.put("attempts",attempts);
            }catch(Exception ignored){}
        }
        return item;
    }

    private JSONObject probeOne(String method,String host,String path){
        JSONObject result=new JSONObject();
        HttpsURLConnection c=null;
        long start=System.currentTimeMillis();
        try{
            URL url=new URL("https://"+host+path);
            c=(HttpsURLConnection)url.openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(3500);c.setReadTimeout(3500);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","PBS-Gwell-Endpoint-Inventory/1.3.3");
            c.setRequestProperty("Accept","application/json");
            int http=c.getResponseCode();
            String allow=safeHeader(c.getHeaderField("Allow"));
            String status=classify(http);
            result.put("method",method);result.put("host",host);result.put("httpStatus",http);
            result.put("status",status);result.put("allowHeader",allow);
            result.put("latencyMs",System.currentTimeMillis()-start);
        }catch(Exception e){
            try{
                String status;
                if(e instanceof UnknownHostException)status="DNS_LOOKUP_FAILED";
                else if(e instanceof SocketTimeoutException)status="NETWORK_TIMEOUT";
                else if(e instanceof SSLException)status="TLS_ERROR";
                else if(e instanceof java.net.ConnectException)status="CONNECTION_ERROR";
                else status="NETWORK_ERROR";
                result.put("method",method);result.put("host",host);
                result.put("status",status);result.put("errorClass",e.getClass().getSimpleName());
                result.put("latencyMs",System.currentTimeMillis()-start);
            }catch(Exception ignored){}
        }finally{if(c!=null)c.disconnect();}
        return result;
    }

    private String classify(int http){
        if(http>=200&&http<300)return "HTTP_2XX_RESPONSE";
        if(http>=300&&http<400)return "HTTP_REDIRECT";
        if(http==401||http==403)return "AUTH_OR_ACCESS_RESTRICTED";
        if(http==404)return "PATH_NOT_FOUND";
        if(http==405||http==501)return "METHOD_NOT_SUPPORTED";
        if(http>=500)return "SERVER_ERROR";
        return "HTTP_RESPONSE_OTHER";
    }
    private int priority(String status){
        if("HTTP_2XX_RESPONSE".equals(status))return 6;
        if("HTTP_REDIRECT".equals(status))return 5;
        if("AUTH_OR_ACCESS_RESTRICTED".equals(status))return 4;
        if("METHOD_NOT_SUPPORTED".equals(status))return 3;
        if("HTTP_RESPONSE_OTHER".equals(status))return 2;
        if("SERVER_ERROR".equals(status))return 1;
        if("PATH_NOT_FOUND".equals(status))return 0;
        if("NETWORK_TIMEOUT".equals(status))return -1;
        if("TLS_ERROR".equals(status)||"CONNECTION_ERROR".equals(status))return -2;
        if("DNS_LOOKUP_FAILED".equals(status))return -3;
        if("NETWORK_ERROR".equals(status))return -4;
        return -5;
    }

    private JSONObject timeoutResult(Endpoint e){
        JSONObject o=new JSONObject();
        try{o.put("group",e.group);o.put("path",e.path);o.put("riskClass",e.risk);
            o.put("hostConfidence",e.hostConfidence);o.put("probeMethods",e.headSafe?"OPTIONS,HEAD":"OPTIONS_ONLY");
            o.put("status","SCAN_TIMEOUT");o.put("requestBodySent",false);o.put("credentialsSent",false);
            o.put("attemptCount",0);o.put("attempts",new JSONArray());
            o.put("details","Batas waktu pemindaian tercapai; endpoint belum dinyatakan tidak tersedia.");}
        catch(Exception ignored){}return o;
    }
    private JSONObject errorResult(Endpoint e,Exception ex){
        JSONObject o=timeoutResult(e);
        try{o.put("status","SCAN_ERROR");o.put("details",safeClass(ex));}catch(Exception ignored){}
        return o;
    }

    private JSONObject summarize(JSONArray results){
        JSONObject s=new JSONObject();
        int ok=0,redirect=0,restricted=0,notFound=0,methodUnsupported=0,serverError=0;
        int networkEndpoints=0,dnsEndpoints=0,timeoutEndpoints=0,tlsEndpoints=0,connectionEndpoints=0;
        int unmapped=0,scanTimeout=0,scanError=0,noResponse=0,other=0,notProbed=0;
        int attemptTotal=0,aOk=0,aRedirect=0,aRestricted=0,aNotFound=0,aMethod=0,aServer=0;
        int aDns=0,aTimeout=0,aTls=0,aConnection=0,aNetwork=0,aOther=0;
        for(int i=0;i<results.length();i++){
            JSONObject r=results.optJSONObject(i);if(r==null)continue;
            String status=r.optString("status");
            if("HTTP_2XX_RESPONSE".equals(status))ok++;
            else if("HTTP_REDIRECT".equals(status))redirect++;
            else if("AUTH_OR_ACCESS_RESTRICTED".equals(status))restricted++;
            else if("PATH_NOT_FOUND".equals(status))notFound++;
            else if("METHOD_NOT_SUPPORTED".equals(status))methodUnsupported++;
            else if("SERVER_ERROR".equals(status))serverError++;
            else if("DNS_LOOKUP_FAILED".equals(status)){dnsEndpoints++;networkEndpoints++;}
            else if("NETWORK_TIMEOUT".equals(status)){timeoutEndpoints++;networkEndpoints++;}
            else if("TLS_ERROR".equals(status)){tlsEndpoints++;networkEndpoints++;}
            else if("CONNECTION_ERROR".equals(status)){connectionEndpoints++;networkEndpoints++;}
            else if("NETWORK_ERROR".equals(status)){networkEndpoints++;}
            else if("NO_RESPONSE".equals(status)){noResponse++;networkEndpoints++;}
            else if("HOST_MAPPING_REQUIRED".equals(status))unmapped++;
            else if("SCAN_TIMEOUT".equals(status))scanTimeout++;
            else if("SCAN_ERROR".equals(status))scanError++;
            else if("NOT_PROBED".equals(status))notProbed++;
            else other++;

            JSONArray attempts=r.optJSONArray("attempts");
            if(attempts==null)continue;
            for(int j=0;j<attempts.length();j++){
                JSONObject a=attempts.optJSONObject(j);if(a==null)continue;
                attemptTotal++;
                String as=a.optString("status");
                if("HTTP_2XX_RESPONSE".equals(as))aOk++;
                else if("HTTP_REDIRECT".equals(as))aRedirect++;
                else if("AUTH_OR_ACCESS_RESTRICTED".equals(as))aRestricted++;
                else if("PATH_NOT_FOUND".equals(as))aNotFound++;
                else if("METHOD_NOT_SUPPORTED".equals(as))aMethod++;
                else if("SERVER_ERROR".equals(as))aServer++;
                else if("DNS_LOOKUP_FAILED".equals(as))aDns++;
                else if("NETWORK_TIMEOUT".equals(as))aTimeout++;
                else if("TLS_ERROR".equals(as))aTls++;
                else if("CONNECTION_ERROR".equals(as))aConnection++;
                else if("NETWORK_ERROR".equals(as))aNetwork++;
                else aOther++;
            }
        }
        JSONObject attemptSummary=new JSONObject();
        try{
            attemptSummary.put("total",attemptTotal);attemptSummary.put("http2xx",aOk);
            attemptSummary.put("redirect",aRedirect);attemptSummary.put("authOrAccessRestricted",aRestricted);
            attemptSummary.put("pathNotFound",aNotFound);attemptSummary.put("methodNotSupported",aMethod);
            attemptSummary.put("serverError",aServer);attemptSummary.put("dnsLookupFailed",aDns);
            attemptSummary.put("networkTimeout",aTimeout);attemptSummary.put("tlsError",aTls);
            attemptSummary.put("connectionError",aConnection);attemptSummary.put("networkError",aNetwork);
            attemptSummary.put("other",aOther);
            s.put("total",results.length());s.put("http2xx",ok);s.put("redirect",redirect);
            s.put("authOrAccessRestricted",restricted);s.put("pathNotFound",notFound);
            s.put("methodNotSupported",methodUnsupported);s.put("serverError",serverError);
            s.put("networkError",networkEndpoints);s.put("dnsLookupFailed",dnsEndpoints);
            s.put("networkTimeout",timeoutEndpoints);s.put("tlsError",tlsEndpoints);
            s.put("connectionError",connectionEndpoints);s.put("noResponse",noResponse);
            s.put("hostMappingRequired",unmapped);s.put("scanTimeout",scanTimeout);
            s.put("scanError",scanError);s.put("other",other);s.put("notProbed",notProbed);
            s.put("attemptSummary",attemptSummary);
        }catch(Exception ignored){}
        return s;
    }

    private boolean isRetryable(String status){
        return "NETWORK_ERROR".equals(status)||"DNS_LOOKUP_FAILED".equals(status)||
            "NETWORK_TIMEOUT".equals(status)||"TLS_ERROR".equals(status)||
            "CONNECTION_ERROR".equals(status)||"SERVER_ERROR".equals(status)||
            "PATH_NOT_FOUND".equals(status);
    }
    private boolean isHttpResponseStatus(String status){
        return "HTTP_2XX_RESPONSE".equals(status)||"HTTP_REDIRECT".equals(status)||
            "AUTH_OR_ACCESS_RESTRICTED".equals(status)||"METHOD_NOT_SUPPORTED".equals(status)||
            "HTTP_RESPONSE_OTHER".equals(status)||"PATH_NOT_FOUND".equals(status)||
            "SERVER_ERROR".equals(status);
    }

    private boolean isPositiveResponse(String status){
        return "HTTP_2XX_RESPONSE".equals(status)||"HTTP_REDIRECT".equals(status)||
            "AUTH_OR_ACCESS_RESTRICTED".equals(status)||"METHOD_NOT_SUPPORTED".equals(status)||
            "HTTP_RESPONSE_OTHER".equals(status);
    }
    private void status(String value){
        runOnUiThread(()->{if(statusView!=null)statusView.setText(value);});
    }
    private String safeHeader(String value){
        if(value==null||value.trim().isEmpty())return "not_returned";
        String clean=value.replaceAll("[^A-Za-z0-9 ,;=._/-]","");
        return clean.length()>120?clean.substring(0,120):clean;
    }
    private String safeClass(Throwable e){
        return e.getClass().getSimpleName()+" (details omitted)";
    }
    private String utcNow(){
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());
    }
    private String makeSummary(JSONObject report){
        JSONObject s=report.optJSONObject("summary");
        if(s==null)return "Pemindaian selesai, laporan ringkas tidak tersedia.";
        return "ENDPOINT INVENTORY SELESAI\n"+
            "Total: "+s.optInt("total")+
            " · HTTP 2xx: "+s.optInt("http2xx")+
            " · Redirect: "+s.optInt("redirect")+
            " · Auth/Access: "+s.optInt("authOrAccessRestricted")+
            " · 404: "+s.optInt("pathNotFound")+
            " · Method unsupported: "+s.optInt("methodNotSupported")+
            " · Network error: "+s.optInt("networkError")+
            " · DNS gagal: "+s.optInt("dnsLookupFailed")+
            " · Timeout: "+s.optInt("networkTimeout")+
            " · Host belum dipetakan: "+s.optInt("hostMappingRequired")+
            "\nStatus: "+report.optString("overall");
    }
    private void copyReport(){
        ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("PBS Gwell Endpoint Inventory",reportText));
        statusView.setText("Laporan JSON disalin. Tidak memuat token, password, Device ID, atau raw response.");
    }
    @Override protected void onDestroy(){
        scanWorker.shutdownNow();networkPool.shutdownNow();super.onDestroy();
    }
}
