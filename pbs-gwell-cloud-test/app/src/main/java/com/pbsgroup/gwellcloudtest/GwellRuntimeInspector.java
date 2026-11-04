package com.pbsgroup.gwellcloudtest;

import java.util.Locale;

/**
 * Read-only classpath verification, without vendor login, network access,
 * camera binding, or evaluating third-party static initializers.
 */
final class GwellRuntimeInspector {
    private static final String[][] CLASSES={
        {"GWELL_P2P", "com.p2p.core.P2PSpecial.P2PSpecial"},
        {"GWELL_PLAYER", "com.p2p.core.P2PHandler"},
        {"GWELL_LOGIN", "com.libhttp.subscribers.SubscriberListener"},
        {"RETROFIT", "retrofit2.Retrofit"},
        {"RETROFIT_GSON", "retrofit2.converter.gson.GsonConverterFactory"},
        {"RETROFIT_RXJAVA2", "com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory"},
        {"OKHTTP", "okhttp3.OkHttpClient"},
        {"OKHTTP_LOGGER", "okhttp3.logging.HttpLoggingInterceptor"},
        {"GSON", "com.google.gson.Gson"},
        {"RXJAVA2", "io.reactivex.Observable"},
        {"RXANDROID2", "io.reactivex.android.schedulers.AndroidSchedulers"}
    };
    private GwellRuntimeInspector(){}

    static String classpathReport(){
        StringBuilder b=new StringBuilder("GWELL_RUNTIME_CLASS_CHECK");
        int missing=0;
        for(String[] pair:CLASSES){
            try{
                Class.forName(pair[1],false,GwellRuntimeInspector.class.getClassLoader());
                b.append("; ").append(pair[0]).append("=FOUND");
            }catch(Throwable failure){
                missing++;
                b.append("; ").append(pair[0]).append("=MISSING");
                b.append("(").append(failure.getClass().getSimpleName()).append(")");
            }
        }
        b.append("; missingCount=").append(missing);
        b.append("; NOTE=class_presence_is_not_vendor_auth_or_native_abi_proof");
        return b.toString();
    }

    static String safeLinkageCause(Throwable error){
        StringBuilder b=new StringBuilder();
        Throwable cause=error;
        for(int i=0;i<4 && cause!=null;i++,cause=cause.getCause()){
            if(i>0)b.append("; ");
            b.append("cause").append(i).append("=").append(cause.getClass().getSimpleName());
            String message=cause.getMessage();
            // No arbitrary vendor exception message: only plausible Java class symbols.
            if(message!=null){
                String normalized=message.trim().replace('/', '.');
                if(normalized.startsWith("Failed resolution of: L") && normalized.endsWith(";")){
                    normalized=normalized.substring("Failed resolution of: L".length(),normalized.length()-1).trim();
                }else if(normalized.startsWith("Could not initialize class ")){
                    normalized=normalized.substring("Could not initialize class ".length()).trim();
                }else if(normalized.startsWith("java.lang.NoClassDefFoundError: ")){
                    normalized=normalized.substring("java.lang.NoClassDefFoundError: ".length()).trim();
                }
                // Prevent exporting keys, personal IDs, URLs, emails or freeform messages.
                if(normalized.length()<140 && normalized.contains(".") &&
                   normalized.matches("[A-Za-z_$][A-Za-z0-9_$.]+") &&
                   !normalized.toLowerCase(Locale.ROOT).contains("token") &&
                   !normalized.toLowerCase(Locale.ROOT).contains("password")){
                    b.append("; missingClass=").append(normalized);
                }
            }
            StackTraceElement[] trace=cause.getStackTrace();
            if(trace!=null){
                for(int n=0;n<Math.min(5,trace.length);n++){
                    String cls=trace[n].getClassName();
                    String method=trace[n].getMethodName();
                    if(cls!=null && cls.matches("[A-Za-z_$][A-Za-z0-9_$.]+") &&
                        method!=null && method.matches("[A-Za-z_$][A-Za-z0-9_$]+")){
                        b.append("; at").append(n).append("=")
                         .append(cls).append(".").append(method)
                         .append(":").append(trace[n].getLineNumber());
                    }
                }
            }
        }
        return b.toString();
    }
}
