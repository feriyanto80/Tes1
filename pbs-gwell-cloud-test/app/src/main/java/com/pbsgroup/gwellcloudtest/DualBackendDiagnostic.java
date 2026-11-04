package com.pbsgroup.gwellcloudtest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import java.net.InetAddress;

/**
 * Dual backend capability assessment. This is an OFFLINE/READ-ONLY
 * capability diagnostic for legacy GWell; no proprietary P2P JNI or
 * unauthorized device authentication protocol is guessed.
 */
final class DualBackendDiagnostic {
    static final class Result {
        final String recommendation;
        final String confidence;
        final String evidence;
        final String legacyRuntime;
        final String legacyCredentialState;
        final String legacyDns;
        final boolean officialYooseePresent;
        Result(String recommendation,String confidence,String evidence,
               String legacyRuntime,String legacyCredentialState,
               String legacyDns,boolean officialYooseePresent) {
            this.recommendation=recommendation;
            this.confidence=confidence;
            this.evidence=evidence;
            this.legacyRuntime=legacyRuntime;
            this.legacyCredentialState=legacyCredentialState;
            this.legacyDns=legacyDns;
            this.officialYooseePresent=officialYooseePresent;
        }

        String report() {
            return "BACKEND_ASSESSMENT: "+recommendation+
                "; confidence="+confidence+
                "; evidence="+evidence+
                "; legacyRuntime="+legacyRuntime+
                "; legacyCredentials="+legacyCredentialState+
                "; legacyDns="+legacyDns+
                "; officialYooseeInstalled="+officialYooseePresent;
        }
    }
    private DualBackendDiagnostic(){}

    static Result run(Context context,String deviceId,P2PAuthorizationProbe.Result cloud){
        String category=cloud==null?"UNKNOWN":cloud.deviceCate;
        String hint=cloud==null?"UNKNOWN":cloud.backendHint;
        boolean legacyNumber=deviceId!=null && deviceId.matches("[0-9]{1,8}");
        String recommendation;
        String confidence;
        String evidence;

        if("GWELL".equals(hint)){
            recommendation="GWELL_LEGACY_CANDIDATE";
            confidence="HIGH_METADATA_HINT_NOT_SESSION_PROOF";
            evidence="explicitBackend=GWELL";
        } else if("IOTVIDEO".equals(hint)){
            recommendation="IOTVIDEO_CANDIDATE";
            confidence="HIGH_METADATA_HINT_NOT_SESSION_PROOF";
            evidence="explicitBackend=IOTVIDEO";
        } else if(legacyNumber && "1".equals(category)){
            recommendation="GWELL_LEGACY_CANDIDATE";
            confidence="MEDIUM_HEURISTIC_NOT_PROOF";
            evidence="numericIdLength<=8; deviceCate=1 (first-generation label in public demo)";
        } else if(legacyNumber){
            recommendation="GWELL_LEGACY_CANDIDATE";
            confidence="LOW_HEURISTIC_ONLY";
            evidence="numericIdLength<=8; backend metadata not explicit";
        } else if("2".equals(category)){
            recommendation="IOTVIDEO_CANDIDATE";
            confidence="LOW_HEURISTIC_ONLY";
            evidence="deviceCate=2; vendor-specific interpretation unverified";
        } else {
            recommendation="UNKNOWN_REQUIRES_VENDOR_METADATA";
            confidence="UNKNOWN";
            evidence="no explicit protocol marker";
        }

        boolean sdkPresent=classPresent("com.p2p.core.P2PHandler");
        String runtime=sdkPresent?"LEGACY_API_CLASS_PRESENT_NATIVE_NOT_VERIFIED":
            "NOT_PACKAGED_NO_LEGACY_P2P_LIVE";
        String credentials="UNAVAILABLE_GWELL_SESSIONID1_SESSIONID2_VERIFYCODE1_VERIFYCODE2";
        boolean yoosee=hasOfficialApp(context);
        String dns=probeDns();

        return new Result(recommendation,confidence,evidence,runtime,credentials,dns,yoosee);
    }

    private static boolean classPresent(String className){
        try {
            Class.forName(className,false,DualBackendDiagnostic.class.getClassLoader());
            return true;
        } catch(Throwable ignored){return false;}
    }

    private static boolean hasOfficialApp(Context context){
        try {
            context.getPackageManager().getPackageInfo("com.yoosee",0);
            return true;
        } catch(PackageManager.NameNotFoundException ignored){return false;}
        catch(Exception ignored){return false;}
    }

    private static String probeDns(){
        // DNS-only checks. Neither proves server availability nor connects to a camera.
        StringBuilder out=new StringBuilder();
        for(String host:new String[]{"p2p1.cloudlinks.cn","p2p3.cloud-links.net",
                                     "list.iotvideo.cloudlinks.cn"}){
            long start=SystemClock.elapsedRealtime();
            String state;
            try{
                InetAddress[] addresses=InetAddress.getAllByName(host);
                state=addresses.length>0?"RESOLVED":"EMPTY";
            }catch(Exception ignored){state="UNRESOLVED";}
            if(out.length()>0)out.append(",");
            out.append(host).append(":").append(state)
               .append("(").append(SystemClock.elapsedRealtime()-start).append("ms)");
        }
        return out.toString();
    }
}
