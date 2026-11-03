package com.pbsgroup.gwellcloudtest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;

final class YooseeApi {
    static final String HOST = "openapi-iot.cloudlinks.cn";
    private static final String ROOT = "https://" + HOST + "/openapi/app/user/";
    private static final String VERSION = "10";
    private final String appId, appToken, uniqueId;
    private String accessTokenFull, accessId;

    YooseeApi(String appId, String appToken) {
        this.appId = appId.trim();
        this.appToken = appToken.trim();
        this.uniqueId = UUID.randomUUID().toString().replace("-", "");
        if (this.appId.isEmpty() || this.appToken.isEmpty())
            throw new IllegalArgumentException("AppID dan AppToken yang sah wajib diisi.");
    }

    boolean hasSession() { return accessTokenFull != null && accessId != null; }
    void close() { accessTokenFull = null; accessId = null; }

    JSONObject login(String account, String password, String countryCode) throws Exception {
        if (account.isEmpty() || password.isEmpty())
            throw new IllegalArgumentException("Masukkan akun dan password yang Anda miliki.");
        JSONObject body = new JSONObject();
        body.put("action", "account");
        body.put("apiVersion", VERSION);
        body.put("appId", appId);
        body.put("appName", "Yoosee.PC");
        body.put("appToken", appToken);
        body.put("appVersion", VERSION);
        body.put("language", "en");
        body.put("pkgName", "Yoosee.PC");
        body.put("pwd", md5(password));
        body.put("terminalOS", 1);
        body.put("uniqueId", uniqueId);
        if (account.matches("^[0-9]+$")) {
            body.put("loginMode", "mobile");
            body.put("mobile", account);
            body.put("mobileArea", countryCode);
        } else {
            body.put("loginMode", "email");
            body.put("email", account);
        }
        JSONObject response = request("POST",ROOT+"login/account",body.toString(),null);
        if (response.optInt("code",-1) == 0) {
            JSONObject data=response.optJSONObject("data");
            if (data==null) throw new IllegalStateException("Login sukses tetapi data sesi kosong.");
            String token=data.optString("accessToken"), id=data.optString("accessId");
            if (token.length() < 128 || id.isEmpty())
                throw new IllegalStateException("Format sesi cloud tidak cocok (SDK/API mungkin berubah).");
            this.accessTokenFull=token;
            this.accessId=id;
        } else { close(); }
        return response;
    }

    JSONObject listDevices() throws Exception {
        return signedGet("device/listDevice", "0", null);
    }
    JSONObject deviceInfo(String deviceId) throws Exception {
        if(!deviceId.matches("^[A-Za-z0-9_\\-]{1,100}$"))
            throw new IllegalArgumentException("Format Device ID tidak valid.");
        return signedGet("device/getDeviceInfo","0",deviceId);
    }

    private JSONObject signedGet(String route, String lastId, String deviceId) throws Exception {
        if (!hasSession()) throw new IllegalStateException("Login ke akun sendiri terlebih dahulu.");
        String tokenUrl=accessTokenFull.substring(0,96);
        TreeMap<String,String> values = new TreeMap<>();
        values.put("accessToken",tokenUrl);
        values.put("apiVersion",VERSION);
        values.put("appId",appId);
        values.put("appName","Yoosee.PC");
        values.put("appToken",appToken);
        values.put("appVersion",VERSION);
        values.put("host",HOST);
        values.put("language","ZH");
        values.put("lastDeviceId",lastId);
        values.put("pkgName","Yoosee.PC");
        values.put("terminalOS","1");
        values.put("x-iotvideo-accessid",accessId);
        String nonce=String.valueOf(new SecureRandom().nextInt(900000)+100000);
        String timestamp=String.valueOf(System.currentTimeMillis()/1000L);
        values.put("x-iotvideo-nonce",nonce);
        values.put("x-iotvideo-timestamp",timestamp);
        StringBuilder canonical=new StringBuilder();
        for(Map.Entry<String,String> e:values.entrySet()){
            if(canonical.length()>0)canonical.append("\n");
            canonical.append(e.getKey()).append(":").append(e.getValue());
        }
        byte[] key=hexDecode(accessTokenFull.substring(96,128));
        Mac mac=Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key,"HmacSHA1"));
        String sig=Base64.getEncoder().encodeToString(mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        TreeMap<String,String> params=new TreeMap<>();
        for(String k:new String[]{"accessToken","apiVersion","appId","appName","appToken",
                "appVersion","language","lastDeviceId","pkgName","terminalOS"})
            params.put(k,values.get(k));
        if(deviceId!=null)params.put("deviceId",deviceId);
        StringBuilder query=new StringBuilder();
        for(Map.Entry<String,String> e:params.entrySet()){
            if(query.length()>0)query.append("&");
            query.append(enc(e.getKey())).append("=").append(enc(e.getValue()));
        }
        TreeMap<String,String> headers=new TreeMap<>();
        headers.put("X-IotVideo-AccessID",accessId);
        headers.put("X-IotVideo-Nonce",nonce);
        headers.put("X-IotVideo-Timestamp",timestamp);
        headers.put("X-IotVideo-Signature",sig);
        return request("GET",ROOT+route+"?"+query,null,headers);
    }

    private static JSONObject request(String method,String address,String body,Map<String,String> headers)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(12000); c.setReadTimeout(12000);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("User-Agent","PBS-Gwell-Cloud-Diagnostic/1.0");
        if(headers!=null)for(Map.Entry<String,String> e:headers.entrySet())
            c.setRequestProperty(e.getKey(),e.getValue());
        if(body!=null){
            c.setRequestProperty("Content-Type","application/json; charset=utf-8");
            c.setDoOutput(true);
            try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
        }
        try{
            int status=c.getResponseCode();
            InputStream in=status>=400?c.getErrorStream():c.getInputStream();
            if(in==null)throw new IllegalStateException("HTTP "+status+" tanpa respons");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(in){
                byte[] buf=new byte[4096];int len;
                while((len=in.read(buf))!=-1){
                    if(out.size()+len>256*1024)throw new IllegalStateException("Respons terlalu besar.");
                    out.write(buf,0,len);
                }
            }
            JSONObject json=new JSONObject(out.toString("UTF-8"));
            json.put("_httpStatus",status);
            return json;
        } finally {c.disconnect();}
    }
    private static byte[] hexDecode(String hex){
        byte[] bytes=new byte[hex.length()/2];
        for(int i=0;i<bytes.length;i++){
            int k=Character.digit(hex.charAt(2*i),16),v=Character.digit(hex.charAt(2*i+1),16);
            if(k<0||v<0)throw new IllegalArgumentException("Token bukan karakter hexadecimal.");
            bytes[i]=(byte)((k<<4)|v);
        }
        return bytes;
    }
    private static String md5(String raw)throws Exception {
        byte[] bytes=MessageDigest.getInstance("MD5").digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();
        for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&0xff));
        return out.toString();
    }
    private static String enc(String value)throws Exception{return URLEncoder.encode(value,"UTF-8");}
}
