package br.ndz.float_button_overlay.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class ApplicationMonitor {
    public static boolean isAppRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfo = activityManager.getRunningAppProcesses();
        if (procInfo != null) {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfo) {
                if (processInfo.processName.equals(packageName)) {
                    return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                }
            }
        }
        return false;
    }

    public static boolean isServiceRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> procInfo = activityManager.getRunningServices(1);
        if (procInfo != null) {
            for (final ActivityManager.RunningServiceInfo processInfo : procInfo) {
                return processInfo.started;
            }
        }
        return false;
    }
}
