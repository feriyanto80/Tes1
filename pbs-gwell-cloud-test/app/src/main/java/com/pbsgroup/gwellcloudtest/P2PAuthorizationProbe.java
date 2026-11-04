package com.pbsgroup.gwellcloudtest;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read-only preflight using the SAME in-memory session as the existing Cloud API test.
 * Never binds/unbinds devices. A missing ID in a potentially paginated list is not
 * proof that a device is unauthorized. Raw responses and credentials are not logged.
 */
final class P2PAuthorizationProbe {
    static final class Result {
        final String state;
        final String detail;
        final boolean listed;
        Result(String state, String detail, boolean listed) {
            this.state=state; this.detail=detail; this.listed=listed;
        }
    }
    private P2PAuthorizationProbe() {}

    static Result check(String deviceId) {
        if (!P2PSession.ready())
            return new Result("NO_SESSION","Login required",false);
        if (blank(P2PSession.appId()) || blank(P2PSession.appToken()))
            return new Result("NO_APP_CREDENTIALS","AppID/AppToken absent: cannot check authorized device list",false);
        YooseeApi api=null;
        try {
            api=new YooseeApi(P2PSession.appId(),P2PSession.appToken());
            api.restoreSession(P2PSession.id(),P2PSession.token());
            JSONObject response=api.listDevices();
            int code=response.optInt("code",-1);
            int http=response.optInt("_httpStatus",-1);
            if (code!=0)
                return new Result("LIST_API_REJECTED","device/listDevice HTTP "+http+" API code "+code,false);
            JSONArray list=extractList(response);
            if (list==null)
                return new Result("LIST_SHAPE_UNKNOWN","device/listDevice accepted; no recognized list array",false);
            for(int i=0;i<list.length();i++){
                JSONObject row=list.optJSONObject(i);
                if (row==null)continue;
                String id=field(row,"deviceId","devId","deviceID","device_id","id");
                if (deviceId.equals(id)) {
                    return new Result("LISTED","Selected device found in authenticated account list; listCount="+list.length()+
                            "; relationFieldPresent="+row.has("relation")+
                            "; playback permission NOT yet proven",true);
                }
            }
            // Lists can be paginated. Test read-only detail before drawing any conclusion.
            String listDetail="Selected ID absent from returned list page (count="+list.length()+"); pagination may apply";
            try {
                JSONObject info=api.deviceInfo(deviceId);
                if(info.optInt("code",-1)==0)
                    return new Result("DETAIL_ACCEPTED",listDetail+
                            "; device/getDeviceInfo accepted; playback permission NOT yet proven",false);
                int error=info.optInt("code",-1);
                int status=info.optInt("_httpStatus",-1);
                return new Result("LIST_MISS_INFO_REJECTED",listDetail+
                        "; getDeviceInfo HTTP "+status+" API code "+error,false);
            }catch(Exception detailError) {
                return new Result("LIST_MISS_INFO_ERROR",listDetail+
                        "; getDeviceInfo error="+detailError.getClass().getSimpleName(),false);
            }
        }catch(Exception e){
            return new Result("PREFLIGHT_UNAVAILABLE","Read-only cloud preflight failed: "+
                    e.getClass().getSimpleName(),false);
        }finally{
            if(api!=null)api.close();
        }
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
    private static boolean blank(String v){return v==null || v.trim().isEmpty();}
}
