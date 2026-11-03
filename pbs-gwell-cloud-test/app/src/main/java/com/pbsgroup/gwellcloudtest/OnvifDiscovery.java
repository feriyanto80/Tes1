package com.pbsgroup.gwellcloudtest;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** WS-Discovery probe over local Wi-Fi only. Not a P2P/cloud camera resolver. */
final class OnvifDiscovery {
    static final class Camera {
        final String address;
        final int port;
        final String xaddr;
        final String endpoint;
        Camera(String address,int port,String xaddr,String endpoint){
            this.address=address;this.port=port;this.xaddr=xaddr;this.endpoint=endpoint;
        }
        String label(){return address+":"+port;}
    }
    private OnvifDiscovery(){}
    static List<Camera> find(Context context)throws Exception {
        WifiManager wifi=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifi==null||!wifi.isWifiEnabled())
            throw new IllegalStateException("Aktifkan WiFi dan sambungkan HP ke ONT kamera untuk discovery lokal.");
        WifiManager.MulticastLock lock=wifi.createMulticastLock("PBS-ONVIF-Discovery");
        lock.setReferenceCounted(false);lock.acquire();
        MulticastSocket socket=null;
        try {
            socket=new MulticastSocket(0);
            socket.setTimeToLive(1);
            socket.setSoTimeout(700);
            String uuid=UUID.randomUUID().toString();
            String payload="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\""+
                " xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\""+
                " xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\""+
                " xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">"+
                "<e:Header><w:MessageID>uuid:"+uuid+"</w:MessageID>"+
                "<w:To e:mustUnderstand=\"true\">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>"+
                "<w:Action e:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>"+
                "</e:Header><e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types>"+
                "</d:Probe></e:Body></e:Envelope>";
            byte[] msg=payload.getBytes(StandardCharsets.UTF_8);
            DatagramPacket probe=new DatagramPacket(msg,msg.length,InetAddress.getByName("239.255.255.250"),3702);
            socket.send(probe);
            Map<String,Camera> cameras=new LinkedHashMap<>();
            long end=System.currentTimeMillis()+4000;
            while(System.currentTimeMillis()<end){
                byte[] buffer=new byte[16384];DatagramPacket packet=new DatagramPacket(buffer,buffer.length);
                try{socket.receive(packet);}catch(SocketTimeoutException e){continue;}
                String xml=new String(packet.getData(),packet.getOffset(),packet.getLength(),StandardCharsets.UTF_8);
                List<String> urls=xaddrs(xml);
                for(String value:urls) {
                    try{
                        URL u=new URL(value);
                        String remoteHost=u.getHost();
                        String src=packet.getAddress().getHostAddress();
                        // Trust packet source, not a SOAP-supplied unrelated host. Avoid SSRF-style arbitrary endpoints.
                        if(!remoteHost.equalsIgnoreCase(src)) continue;
                        if(!isPrivateIp(src))continue;
                        int port=u.getPort()>0?u.getPort():80;
                        if(port<1||port>65535)continue;
                        cameras.put(src+":"+port,new Camera(src,port,value,src));
                    }catch(Exception ignored){}
                }
            }
            return new ArrayList<>(cameras.values());
        }finally{
            if(socket!=null)socket.close();
            if(lock.isHeld())lock.release();
        }
    }
    private static boolean isPrivateIp(String s){
        String[] x=s.split("\\.");
        if(x.length!=4)return false;
        try {
            int a=Integer.parseInt(x[0]),b=Integer.parseInt(x[1]);
            return a==10||(a==172&&b>=16&&b<=31)||(a==192&&b==168);
        }catch(Exception ex){return false;}
    }
    private static List<String> xaddrs(String xml){
        if(xml.length()>16384)return Collections.emptyList();
        List<String> found=new ArrayList<>();
        try{
            XmlPullParser p=Xml.newPullParser();
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);
            p.setInput(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),"UTF-8");
            int event=p.getEventType();
            while(event!=XmlPullParser.END_DOCUMENT){
                if(event==XmlPullParser.START_TAG&&"XAddrs".equals(p.getName())){
                    String body=p.nextText();
                    for(String v:body.trim().split("\\s+"))if(v.startsWith("http://")&&v.length()<300)found.add(v);
                }
                event=p.next();
            }
        }catch(Exception ignored){}
        return found;
    }
}
