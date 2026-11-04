package com.pbsgroup.gwellcloudtest;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Authenticated read-only device visibility test.
 * A device appearing in a cloud list is NOT proof of IoTVideo P2P entitlement.
 * No automatic binding, sharing, mutation, raw payload logging or token export.
 */
final class P2PAuthorizationProbe {
    static final class Result {
        final String state;
        final String detail;
        final boolean listed;
        final String relationCode;
        final String deviceCate;
        final String backendHint;

        Result(String state, String detail, boolean listed) {
            this(state, detail, listed, "UNKNOWN", "UNKNOWN", "UNKNOWN");
        }
        Result(String state, String detail, boolean listed,
               String relationCode, String deviceCate, String backendHint) {
            this.state=state;
            this.detail=detail;
            this.listed=listed;
            this.relationCode=relationCode;
            this.deviceCate=deviceCate;
            this.backendHint=backendHint;
        }
    }

    private P2PAuthorizationProbe() {}

    static Result check(String deviceId) {
        if (!P2PSession.ready())
            return new Result("NO_SESSION","Login required",false);
        if (blank(P2PSession.appId()) || blank(P2PSession.appToken()))
            return new Result("NO_APP_CREDENTIALS","Missing AppID/AppToken for read-only list",false);
        YooseeApi api=null;
        try {
            api=new YooseeApi(P2PSession.appId(),P2PSession.appToken());
            api.restoreSession(P2PSession.id(),P2PSession.token());
            JSONObject response=api.listDevices();
            int code=response.optInt("code",-1), http=response.optInt("_httpStatus",-1);
            if(code!=0)
                return new Result("LIST_API_REJECTED","device/listDevice HTTP "+http+" API code "+code,false);
            JSONArray list=extractList(response);
            if(list==null)
                return new Result("LIST_SHAPE_UNKNOWN",
                    "device/listDevice accepted; no recognized list array",false);
            for(int i=0;i<list.length();i++){
                JSONObject row=list.optJSONObject(i);
                if(row==null)continue;
                String id=field(row,"deviceId","devId","deviceID","device_id","id");
                if(deviceId.equals(id)){
                    return fromRow("LISTED",
                        "Selected device in authenticated list; listCount="+list.length()+
                        "; playback entitlement NOT proven",true,row);
                }
            }
            // The server may return only the first page. A miss cannot establish no binding.
            String listDetail="Selected ID absent from first returned list page; count="+list.length()+
                "; pagination/other account namespaces may apply";
            try {
                JSONObject info=api.deviceInfo(deviceId);
                if(info.optInt("code",-1)==0){
                    JSONObject data=info.optJSONObject("data");
                    if(data!=null) return fromRow("DETAIL_ACCEPTED",listDetail+
                        "; device/getDeviceInfo accepted; streaming permission NOT proven",false,data);
                    return new Result("DETAIL_ACCEPTED",listDetail+
                        "; device/getDeviceInfo accepted without recognized metadata",false);
                }
                return new Result("LIST_MISS_INFO_REJECTED",listDetail+
                    "; getDeviceInfo HTTP "+info.optInt("_httpStatus",-1)+
                    " API code "+info.optInt("code",-1),false);
            }catch(Exception e){
                return new Result("LIST_MISS_INFO_ERROR",listDetail+
                    "; getDeviceInfo="+e.getClass().getSimpleName(),false);
            }
        }catch(Exception e){
            return new Result("PREFLIGHT_UNAVAILABLE","Cloud preflight failed: "+
                e.getClass().getSimpleName(),false);
        }finally{
            if(api!=null)api.close();
        }
    }

    private static Result fromRow(String state,String description,boolean listed,JSONObject row){
        String relation=numericSmall(field(row,"relation","relationType","userRelation"));
        String cate=numericSmall(field(row,"deviceCate","devCate","deviceCategory"));
        String backend=backendValue(field(row,"p2pProtocol","protocol","deviceProtocol",
            "backend","sdkType"));
        return new Result(state,description+
            "; relationCode="+relation+"; deviceCate="+cate+
            "; protocolHint="+backend,
            listed,relation,cate,backend);
    }
    private static String numericSmall(String value){
        return value.matches("[0-9]{1,4}")?value:"UNKNOWN";
    }
    private static String backendValue(String value){
        String upper=value.trim().toUpperCase(java.util.Locale.ROOT);
        // Whitelisted only: never echo freeform API fields or identifiers in reports.
        if(upper.equals("GWELL")||upper.equals("LEGACY")||upper.equals("P2P_CORE"))return "GWELL";
        if(upper.equals("IOTVIDEO")||upper.equals("IOT_VIDEO")||upper.equals("IOT"))return "IOTVIDEO";
        return "UNKNOWN";
    }
    private static String field(JSONObject row,String... keys){
        for(String key:keys){
            String val=row.optString(key,"").trim();
            if(!val.isEmpty() && !"null".equalsIgnoreCase(val))return val;
        }
        return "";
    }
    private static JSONArray extractList(JSONObject response){
        JSONObject data=response.optJSONObject("data");
        if(data!=null){
            for(String key:new String[]{"deviceList","devices","list","records"}){
                JSONArray arr=data.optJSONArray(key);
                if(arr!=null)return arr;
            }
        }
        JSONArray direct=response.optJSONArray("data");
        if(direct!=null)return direct;
        return response.optJSONArray("deviceList");
    }
    private static boolean blank(String v){return v==null||v.trim().isEmpty();}
}
