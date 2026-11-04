package com.pbsgroup.gwellcloudtest;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import com.tencentcs.iotvideo.IoTVideoSdk;
import com.tencentcs.iotvideo.utils.UrlHelper;
import java.io.File;

/** Initializes the native IoTVideo runtime once, before any player activity is opened. */
public final class PbsGwellApplication extends Application {
    private static final String TAG = "PBS-GWELL-SDK";
    private static volatile String initStatus = "SDK_NOT_INITIALIZED";

    public static String getInitStatus() { return initStatus; }

    @Override public void onCreate() {
        super.onCreate();
        final String proc=Build.VERSION.SDK_INT>=28 ? Application.getProcessName() : "";
        final boolean nativeProcess=proc!=null && proc.endsWith(":gwell");
        final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try{NativeCrashLedger.fatal(this,error,nativeProcess);}catch(Throwable ignored){}
            if(previous!=null)previous.uncaughtException(thread,error);
            else android.os.Process.killProcess(android.os.Process.myPid());
        });
        // IoTVideo init is unrelated to GWell and must not run inside isolated GWell process.
        if(nativeProcess){
            initStatus="GWELL_ISOLATED; IOTVIDEO_INIT_SKIPPED";
            return;
        }
        try {
            // The upstream IoTVideo demo selects the release environment by default.
            UrlHelper.getInstance().setServerType(UrlHelper.SERVER_RELEASE);
            IoTVideoSdk.init(this, null);

            File logDir = new File(getFilesDir(), "iotvideo-sdk-logs");
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.w(TAG, "Unable to create SDK log directory");
            }
            IoTVideoSdk.setLogPath(logDir.getAbsolutePath());
            IoTVideoSdk.setDebugMode(IoTVideoSdk.LOG_LEVEL_DEBUG);

            initStatus = "SDK_INIT_OK; SERVER_RELEASE; DEBUG_LOG_ENABLED";
            Log.i(TAG, initStatus);
        } catch (Throwable t) {
            initStatus = "SDK_INIT_FAILED: " + t.getClass().getSimpleName() +
                    (t.getMessage() == null ? "" : " — " + t.getMessage());
            Log.e(TAG, initStatus, t);
        }
    }
}
