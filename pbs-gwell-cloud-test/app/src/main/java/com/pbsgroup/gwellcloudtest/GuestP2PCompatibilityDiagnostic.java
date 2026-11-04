package com.pbsgroup.gwellcloudtest;

import android.content.Context;
import android.content.pm.PackageManager;
import com.tencentcs.iotvideo.IoTVideoSdk;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Guest P2P authentication compatibility evidence, not an authentication bypass.
 * Does not read official Yoosee private data or export tokens/passwords/device IDs.
 * Only checks current PBS in-memory session shape and documented Cloud API results.
 */
final class GuestP2PCompatibilityDiagnostic {
    static final class Result {
        final String guestStatus;
        final String conclusion;
        final String detail;
        Result(String guestStatus, String conclusion, String detail) {
            this.guestStatus=guestStatus;
            this.conclusion=conclusion;
            this.detail=detail;
        }
        String report(){
            return "GUEST_P2P_COMPAT: guestStatus="+guestStatus+
                    "; conclusion="+conclusion+"; "+detail;
        }
    }

    private GuestP2PCompatibilityDiagnostic(){}

    static Result inspect(Context context, P2PAuthorizationProbe.Result cloud,
                          boolean sameAccountAndGuestLiveConfirmed) {
        String relation=cloud==null?"UNKNOWN":cloud.relationCode;
        boolean listed=cloud!=null && cloud.listed;
        boolean guestLike="2".equals(relation);
        boolean officialInstalled=isOfficialAppPresent(context);
        boolean sessionPresent=P2PSession.ready();
        boolean appIdPresent=nonEmpty(P2PSession.appId());
        boolean appTokenPresent=nonEmpty(P2PSession.appToken());
        boolean idNumeric64=isSignedLong(P2PSession.id());
        String accessTokenClass=tokenClass(P2PSession.token());
        String sdkInspection=sdkInspection();

        String guestStatus=guestLike?"GUEST_LIKE_RELATION_CODE_2":
                "1".equals(relation)?"OWNER_LIKE_RELATION_CODE_1":"RELATION_UNKNOWN";
        // This owner/guest mapping originates in the public GWTimes demo and
        // is not guaranteed for this specific Cloudlinks API endpoint.
        String conclusion;
        if(guestLike && listed && sameAccountAndGuestLiveConfirmed){
            conclusion="GUEST_CLOUD_LISTED_AND_OFFICIAL_LIVE_USER_CONFIRMED__"+
                    "PBS_IOTVIDEO_SESSION_REQUIRES_SEPARATE_AUTH_VALIDATION";
        }else if(guestLike && listed){
            conclusion="GUEST_CLOUD_LISTED__OFFICIAL_GUEST_LIVE_NOT_CONFIRMED_IN_APP";
        }else if(listed){
            conclusion="DEVICE_LISTED__STREAMING_ENTITLEMENT_NOT_VERIFIED";
        }else{
            conclusion="DEVICE_RELATIONSHIP_UNKNOWN__BACKEND_ENTITLEMENT_NOT_VERIFIED";
        }
        String detail="cloudListed="+listed+
            "; guestMapping=GWTimes_example_not_authoritative_for_Cloudlinks"+
            "; officialYooseeInstalled="+officialInstalled+
            "; userConfirmedSameAccountAndGuestLive="+sameAccountAndGuestLiveConfirmed+
            "; pbsAppPackage="+context.getPackageName()+
            "; officialAppPackage=com.yoosee"+
            "; packagesDiffer="+!context.getPackageName().equals("com.yoosee")+
            "; pbsAppIdPresent="+appIdPresent+
            "; pbsAppTokenPresent="+appTokenPresent+
            "; cloudSessionPresent="+sessionPresent+
            "; sdkAccessIdSigned64Valid="+idNumeric64+
            "; sdkTokenShape="+accessTokenClass+
            "; vendorSdkApi="+sdkInspection+
            "; sdkRegisterCallReturnNotAuthProof=true"+
            "; appProductNamespaceAndP2PEntitlement=NOT_VERIFIED"+
            "; officialYooseePrivateTokens=NOT_READ";
        return new Result(guestStatus,conclusion,detail);
    }

    private static String sdkInspection(){
        boolean registerVoid=false, callbackCandidate=false, stateCandidate=false;
        try{
            for(Method m:IoTVideoSdk.class.getMethods()){
                String name=m.getName().toLowerCase(Locale.ROOT);
                if("register".equals(name) && m.getParameterTypes().length==2
                    && m.getReturnType()==Void.TYPE) registerVoid=true;
                if((name.contains("register")||name.contains("login"))
                    && (name.contains("listener")||name.contains("callback")))
                    callbackCandidate=true;
                if((name.contains("register")||name.contains("login"))
                    && (name.contains("state")||name.contains("status")))
                    stateCandidate=true;
            }
        }catch(Throwable ignored){
            return "REFLECTION_UNAVAILABLE";
        }
        return "registerVoid="+registerVoid+
            ",authCallbackCandidate="+callbackCandidate+
            ",authStateReaderCandidate="+stateCandidate+
            ",noUndocumentedMethodsInvoked=true";
    }

    private static boolean isOfficialAppPresent(Context c){
        try{c.getPackageManager().getPackageInfo("com.yoosee",0);return true;}
        catch(PackageManager.NameNotFoundException e){return false;}
        catch(Exception e){return false;}
    }
    private static boolean isSignedLong(String s){
        try{return s!=null && !s.isEmpty() && Long.parseLong(s)!=0L;}
        catch(Exception ignored){return false;}
    }
    private static boolean nonEmpty(String s){return s!=null&&!s.trim().isEmpty();}
    private static String tokenClass(String token){
        if(token==null||token.isEmpty())return "MISSING";
        if(token.length()<96)return "PRESENT_SHORT_FORMAT_UNVERIFIED";
        if(token.length()<128)return "PRESENT_MEDIUM_FORMAT_UNVERIFIED";
        return "PRESENT_LENGTH_GE_128_FORMAT_UNVERIFIED";
    }
}
