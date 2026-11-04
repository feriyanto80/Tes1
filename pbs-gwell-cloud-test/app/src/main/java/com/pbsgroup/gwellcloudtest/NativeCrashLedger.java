package com.pbsgroup.gwellcloudtest;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * No vendor SDK dependencies. Persists only redacted lifecycle/crash
 * evidence to app-private storage, readable after a native process restart.
 */
public final class NativeCrashLedger {
    private static final String STATE_FILE="pbs_gwell_native_state.txt";
    private static final String CRASH_FILE="pbs_gwell_native_crash.txt";
    private NativeCrashLedger() {}

    public static void launching(Context ctx) {
        write(ctx,STATE_FILE,"STATE=NATIVE_START_REQUESTED\nBuild=1.3.2\nAt="+System.currentTimeMillis());
        // A new test must not be confused with a previous Java exception.
        try {new File(ctx.getFilesDir(),CRASH_FILE).delete();}catch(Exception ignored){}
    }

    public static void opened(Context ctx) {
        write(ctx,STATE_FILE,"STATE=NATIVE_ACTIVITY_ONCREATE_OK\nBuild=1.3.2\nAt="+System.currentTimeMillis());
    }

    public static void uiReady(Context ctx) {
        write(ctx,STATE_FILE,"STATE=NATIVE_UI_READY\nBuild=1.3.2\nAt="+System.currentTimeMillis());
    }

    public static void exited(Context ctx) {
        write(ctx,STATE_FILE,"STATE=NATIVE_EXITED\nBuild=1.3.2\nAt="+System.currentTimeMillis());
    }

    public static void fatal(Context ctx, Throwable failure, boolean nativeProcess) {
        StringBuilder summary=new StringBuilder("PBS_Native_Crash_v1.3.2\n");
        summary.append("process=").append(nativeProcess?"GWELL_ISOLATED":"MAIN").append("\n");
        summary.append("utcEpochMillis=").append(System.currentTimeMillis()).append("\n");
        summary.append("androidApi=").append(Build.VERSION.SDK_INT).append("\n");
        summary.append("exception=").append(failure==null?"UNKNOWN":failure.getClass().getName()).append("\n");
        Throwable cause=failure;
        int depth=0;
        while(cause!=null && depth++<3) {
            summary.append("cause").append(depth).append("=").append(cause.getClass().getName()).append("\n");
            StackTraceElement[] trace=cause.getStackTrace();
            for(int i=0;i<Math.min(trace.length,9);i++) {
                StackTraceElement element=trace[i];
                // No exception messages, method arguments, account IDs, paths or tokens.
                summary.append("at ").append(element.getClassName()).append(".")
                    .append(element.getMethodName()).append(":")
                    .append(element.getLineNumber()).append("\n");
            }
            cause=cause.getCause();
        }
        write(ctx,CRASH_FILE,summary.toString());
        write(ctx,STATE_FILE,"STATE=JAVA_FATAL_RECORDED\nBuild=1.3.2\nAt="+System.currentTimeMillis());
    }

    public static String report(Context ctx) {
        String state=read(ctx,STATE_FILE);
        String crash=read(ctx,CRASH_FILE);
        return "PBS Native Startup Guard v1.3.2\n"+
            "androidApi="+Build.VERSION.SDK_INT+"\n"+
            "supportedAbis="+abiSummary()+"\n"+
            "state="+(state.isEmpty()?"NO_NATIVE_START_RECORDED":state)+"\n"+
            "javaCrash="+(crash.isEmpty()?"NOT_RECORDED (native SIGSEGV/abort requires adb logcat)":crash)+
            "\nCredentials, SDK tokens, passwords and full device IDs excluded.";
    }

    private static String abiSummary() {
        String[] abis=Build.SUPPORTED_ABIS;
        if(abis==null||abis.length==0)return "UNKNOWN";
        StringBuilder b=new StringBuilder();
        for(int i=0;i<Math.min(abis.length,5);i++) {
            if(i>0)b.append(",");
            String a=abis[i];
            if(a!=null&&a.matches("[A-Za-z0-9_-]{1,32}"))b.append(a);
        }
        return b.toString();
    }

    private static void write(Context ctx,String name,String value){
        try(FileOutputStream out=new FileOutputStream(new File(ctx.getFilesDir(),name),false)){
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }catch(Throwable ignored){}
    }

    private static String read(Context ctx,String name){
        File file=new File(ctx.getFilesDir(),name);
        if(!file.isFile()||file.length()>12000)return "";
        try(FileInputStream in=new FileInputStream(file)){
            byte[] bytes=new byte[(int)file.length()];
            int got=in.read(bytes);
            return got>0?new String(bytes,0,got,StandardCharsets.UTF_8):"";
        }catch(Throwable ignored){return "";}
    }
}
