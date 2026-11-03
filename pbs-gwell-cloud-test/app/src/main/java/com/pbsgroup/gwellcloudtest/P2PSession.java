package com.pbsgroup.gwellcloudtest;

/** Ephemeral auth handoff for a single owned Yoosee account. No disk or intent tokens. */
final class P2PSession {
    private static String accessId,accessToken;
    private P2PSession(){}
    static synchronized void save(String id,String token){
        accessId=id;accessToken=token;
    }
    static synchronized boolean ready(){
        return accessId!=null && !accessId.isEmpty() && accessToken!=null && !accessToken.isEmpty();
    }
    static synchronized String id(){return accessId;}
    static synchronized String token(){return accessToken;}
    static synchronized void clear(){accessId=null;accessToken=null;}
}
