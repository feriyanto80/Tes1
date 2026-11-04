package com.pbsgroup.gwellcloudtest;

/**
 * Compatibility shim for pre-Android-14 third-party GWell SDK receiver
 * registrations. Only retries the specific platform missing-export-flag
 * SecurityException; other security failures are never suppressed.
 *
 * Per Android developer behavior changes for targetSdk >= 34, dynamically
 * registered non-system BroadcastReceivers must explicitly specify
 * RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED.
 *
 * Legacy in-app SDK broadcasts are retried as NOT_EXPORTED, preserving
 * system-only broadcast registrations through the original overload.
 */
final class GwellReceiverCompat {
    private GwellReceiverCompat() {}

    static boolean isMissingExportFlag(SecurityException failure) {
        String message=failure.getMessage();
        if(message==null)return false;
        return message.contains("RECEIVER_EXPORTED") &&
               message.contains("RECEIVER_NOT_EXPORTED");
    }
}
