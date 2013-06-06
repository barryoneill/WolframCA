package net.nologin.meep.ca.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.util.Log;
import net.nologin.meep.ca.SettingsActivity;

import java.text.DecimalFormat;

public class Utils {


    public static final String LOG_TAG = "WolframCA";

    public static void log(String msg){
        Log.d(LOG_TAG,msg);
    }

    public static void log(String msg, Throwable t){
        Log.d(LOG_TAG,msg,t);
    }


    public static class Prefs {

        private static SharedPreferences getPrefs(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx);
        }


        public static boolean getPrefDebugEnabled(Context ctx) {

            return getPrefs(ctx).getBoolean(SettingsActivity.PREF_KEY_SHOW_DEBUG,false);
        }
    }

    public static int roundZoomLevel(int val) {

        int step = 2, min = 1, max = 16;

        val = (Math.round(val/step))*step;

        if(val <= min) {
            return min;
        }
        if(val >= max) {
            return max;
        }

        return val;
    }

    public static String getAppVersionName(Context ctx) {

        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0);
            return info.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG,"Error getting version number! " + e.getMessage(), e);
            return "";
        }

    }
}
