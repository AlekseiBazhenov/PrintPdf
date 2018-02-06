package ru.profit.printpdf.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import ru.profit.printpdf.R;
import timber.log.Timber;

// TODO: 16.03.17 inject for all
public class AppUtils {

    public boolean serviceIsRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hpEprintOnTop(Context context) {
        String HP_EPRINT_PACKAGE = "com.hp.android.print";

        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String onTop = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
        return onTop.equals(HP_EPRINT_PACKAGE);
    }

    public static void changeWiFiState(Context context, boolean state) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        boolean wifiEnabled = wifiManager.isWifiEnabled();
        wifiManager.setWifiEnabled(state);
    }
}
