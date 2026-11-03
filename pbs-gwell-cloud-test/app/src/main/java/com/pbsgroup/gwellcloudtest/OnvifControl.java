package com.pbsgroup.gwellcloudtest;

import java.io.ByteArrayInputStream;
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
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** ONVIF SOAP PTZ for IP-reachable cameras; no Gwell P2P negotiation here. */
final class OnvifControl {
    private final String host,user,password;
    private final int port;
    private String mediaPath="/onvif/media_service";
    private String ptzPath="/onvif/ptz_service";
    private String token="IPCProfilesToken0";
    private String lastPathUsed="";

    OnvifControl(String host,int port,String user,String password){
        this.host=host;this.port=port;this.user=user;this.password=password;
    }
    String profileToken(){return token;}
    String lastEndpoint(){return lastPathUsed;}

    String probe()throws Exception{
        String[] paths={"/onvif/media_service","/onvif/Media","/onvif/device_service"};
        Exception last=null;
        String operation="<trt:GetProfiles/>";
        for(String path:paths){
            try{
                String xml=post(path,operation);
                Document d=parse(xml);
                NodeList profiles=d.getElementsByTagNameNS("*","Profiles");
                if(profiles.getLength()>0){
                    Element profile=(Element)profiles.item(0);
                    String t=profile.getAttribute("token");
                    if(t!=null&&!t.isEmpty()) token=t;
                    mediaPath=path;
                    String capability=readPtzEndpoint();
                    return "ONVIF terhubung. ProfileToken: "+token+
                        "\nMedia: "+mediaPath+"\nPTZ: "+ptzPath+
                        (capability.isEmpty()?"\nEndpoint PTZ default (belum dibuktikan).":"\nEndpoint ditemukan dari GetCapabilities.");
                }
                last=new Exception("GetProfiles tidak mengembalikan token pada "+path);
            }catch(Exception e){last=e;}
        }
        throw new Exception("ONVIF GetProfiles gagal. "+(last==null?"":"Terakhir: "+last.getMessage()));
    }
    private String readPtzEndpoint(){
        try{
            String s=post("/onvif/device_service","<tds:GetCapabilities><tds:Category>PTZ</tds:Category></tds:GetCapabilities>");
            Document d=parse(s);
            NodeList ptz=d.getElementsByTagNameNS("*","PTZ");
            for(int i=0;i<ptz.getLength();i++){
                Element e=(Element)ptz.item(i);
                NodeList xs=e.getElementsByTagNameNS("*","XAddr");
                if(xs.getLength()>0){
                    String found=xs.item(0).getTextContent().trim();
                    URL u=new URL(found);
                    String h=u.getHost();
                    // Prevent using an arbitrary cloud endpoint returned by the device.
                    if(h.equals(host)||h.startsWith("10.")||h.startsWith("192.168.")||h.startsWith("172.")){
                        ptzPath=u.getPath();return found;
                    }
                }
            }
        }catch(Exception ignored){}
        return "";
    }
    static final class StreamInfo {
        final int port;
        final String path;
        StreamInfo(int port,String path){this.port=port;this.path=path;}
    }
    /** Ask ONVIF for actual RTSP URI; never reuse untrusted returned host. */
    StreamInfo discoverRtsp() throws Exception {
        String request="<trt:GetStreamUri><trt:StreamSetup>"+
            "<tt:Stream>RTP-Unicast</tt:Stream>"+
            "<tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport>"+
            "</trt:StreamSetup><trt:ProfileToken>"+escape(token)+
            "</trt:ProfileToken></trt:GetStreamUri>";
        String response=post(mediaPath,request);
        Document document=parse(response);
        if(document.getElementsByTagNameNS("*","Fault").getLength()>0)
            throw new Exception("ONVIF GetStreamUri dikembalikan sebagai SOAP Fault.");
        NodeList uriNodes=document.getElementsByTagNameNS("*","Uri");
        for(int i=0;i<uriNodes.getLength();i++){
            String value=uriNodes.item(i).getTextContent().trim();
            if(value.startsWith("rtsp://")){
                java.net.URI uri=new java.net.URI(value);
                String path=uri.getRawPath();
                if(path==null || path.isEmpty())path="/";
                if(uri.getRawQuery()!=null)path+="?"+uri.getRawQuery();
                return new StreamInfo(uri.getPort()>0?uri.getPort():554,path);
            }
        }
        throw new Exception("ONVIF GetStreamUri tidak berisi URL RTSP. Firmware mungkin tidak mendukung layanan Media.");
    }

    String move(float x,float y)throws Exception{
        String payload="<tptz:ContinuousMove><tptz:ProfileToken>"+escape(token)+
            "</tptz:ProfileToken><tptz:Velocity><tt:PanTilt x=\""+x+
            "\" y=\""+y+"\"/></tptz:Velocity></tptz:ContinuousMove>";
        return sendPtz(payload);
    }
    String stop()throws Exception{
        return sendPtz("<tptz:Stop><tptz:ProfileToken>"+escape(token)+
            "</tptz:ProfileToken><tptz:PanTilt>true</tptz:PanTilt><tptz:Zoom>true</tptz:Zoom></tptz:Stop>");
    }
    private String sendPtz(String xml)throws Exception{
        Exception last=null;
        String[] paths={ptzPath,"/onvif/PTZ","/onvif/ptz_service","/onvif/device_service"};
        for(String path:paths){
            if(path==null||path.isEmpty())continue;
            try{
                String r=post(path,xml);
                Document d=parse(r);
                if(d.getElementsByTagNameNS("*","Fault").getLength()>0)
                    throw new Exception("SOAP Fault dari perangkat.");
                ptzPath=path;return "PTZ request diterima via "+path;
            }catch(Exception e){last=e;}
        }
        throw new Exception("Perintah PTZ gagal. "+(last==null?"":last.getMessage()));
    }
    private String post(String path,String operation)throws Exception{
        if(path==null||!path.startsWith("/")||path.contains(".."))throw new IllegalArgumentException("Path ONVIF tidak valid.");
        String xml="<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\""+
            " xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\""+
            " xmlns:trt=\"http://www.onvif.org/ver10/media/wsdl\""+
            " xmlns:tptz=\"http://www.onvif.org/ver20/ptz/wsdl\""+
            " xmlns:tt=\"http://www.onvif.org/ver10/schema\""+
            " xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""+
            " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"+
            "<s:Header>"+authHeader()+"</s:Header><s:Body>"+operation+"</s:Body></s:Envelope>";
        HttpURLConnection c=(HttpURLConnection)new URL("http://"+host+":"+port+path).openConnection();
        c.setRequestMethod("POST");c.setConnectTimeout(3000);c.setReadTimeout(3500);
        c.setRequestProperty("Content-Type","application/soap+xml; charset=utf-8");
        c.setDoOutput(true);c.setUseCaches(false);
        if(!user.isEmpty()){
            String basic=Base64.getEncoder().encodeToString((user+":"+password).getBytes(StandardCharsets.UTF_8));
            c.setRequestProperty("Authorization","Basic "+basic);
        }
        try{
            byte[] bytes=xml.getBytes(StandardCharsets.UTF_8);
            try(OutputStream out=c.getOutputStream()){out.write(bytes);}
            int status=c.getResponseCode();
            InputStream in=status>=400?c.getErrorStream():c.getInputStream();
            if(in==null)throw new Exception("HTTP "+status+" (tanpa body)");
            ByteArrayOutputStream baos=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            try(InputStream res=in){
                while((n=res.read(buffer))!=-1){
                    if(baos.size()+n>128*1024)throw new Exception("Response terlalu besar");
                    baos.write(buffer,0,n);
                }
            }
            if(status>=400)throw new Exception("HTTP "+status+"; cek user/password/port ONVIF. Digest HTTP mungkin diperlukan.");
            lastPathUsed=path;
            return baos.toString("UTF-8");
        }finally{c.disconnect();}
    }
    private String authHeader()throws Exception{
        if(user.isEmpty())return "";
        byte[] nonce=new byte[16];new SecureRandom().nextBytes(nonce);
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));String created=f.format(new Date());
        ByteArrayOutputStream pre=new ByteArrayOutputStream();
        pre.write(nonce);pre.write(created.getBytes(StandardCharsets.UTF_8));
        pre.write(password.getBytes(StandardCharsets.UTF_8));
        String digest=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(pre.toByteArray()));
        return "<wsse:Security s:mustUnderstand=\"1\"><wsse:UsernameToken>"+
            "<wsse:Username>"+escape(user)+"</wsse:Username>"+
            "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">"+
            digest+"</wsse:Password><wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">"+
            Base64.getEncoder().encodeToString(nonce)+"</wsse:Nonce><wsu:Created>"+created+
            "</wsu:Created></wsse:UsernameToken></wsse:Security>";
    }
    private static String escape(String s){
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");
    }
    private static Document parse(String xml)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        f.setXIncludeAware(false);f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
