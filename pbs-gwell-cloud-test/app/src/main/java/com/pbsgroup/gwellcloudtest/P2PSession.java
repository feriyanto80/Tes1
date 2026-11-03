package com.pbsgroup.gwellcloudtest;

/** Ephemeral session handoff. Credentials exist only in process memory. */
final class P2PSession {
    private static String accessId,accessToken,appId,appToken;
    private P2PSession(){}
    static synchronized void save(String id,String token){
        save(id,token,appId,appToken);
    }
    static synchronized void save(String id,String token,String applicationId,String applicationToken){
        accessId=id;accessToken=token;appId=applicationId;appToken=applicationToken;
    }
    static synchronized boolean ready(){
        return accessId!=null&&!accessId.isEmpty()&&accessToken!=null&&!accessToken.isEmpty();
    }
    static synchronized String id(){return accessId;}
    static synchronized String token(){return accessToken;}
    static synchronized String appId(){return appId;}
    static synchronized String appToken(){return appToken;}
    static synchronized void clear(){
        accessId=null;accessToken=null;appId=null;appToken=null;
    }
}
