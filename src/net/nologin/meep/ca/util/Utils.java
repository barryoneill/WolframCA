package net.nologin.meep.ca.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.util.Log;
import net.nologin.meep.ca.SettingsActivity;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class Utils {


    public static final String LOG_TAG = "WolframCA";

    public static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    public static void log(String msg, Throwable t) {
        Log.d(LOG_TAG, msg, t);
    }


    public static class Prefs {

        private static SharedPreferences getPrefs(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx);
        }


        public static boolean getPrefDebugEnabled(Context ctx) {

            return getPrefs(ctx).getBoolean(SettingsActivity.PREF_KEY_SHOW_DEBUG, false);
        }
    }

    public static int roundZoomLevel(int val) {

        int step = 2, min = 1, max = 16;

        val = (Math.round(val / step)) * step;

        if (val <= min) {
            return min;
        }
        if (val >= max) {
            return max;
        }

        return val;
    }

    public static String getAppVersionName(Context ctx) {

        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return info.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Error getting version number! " + e.getMessage(), e);
            return "";
        }

    }

    /**
     * This code by user 'David' on StackOverflow:
     * http://stackoverflow.com/a/10377934/276183
     * <p/>
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if failed to get result
     */
    public static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                return (Pattern.matches("cpu[0-9]", pathname.getName()));
            }
        }

        try {
            File dir = new File("/sys/devices/system/cpu/");
            File[] files = dir.listFiles(new CpuFilter());

            Log.e(Utils.LOG_TAG, "NUM CORESSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS " + files.length);
            return files.length;
        } catch (Exception e) {

            Log.e(Utils.LOG_TAG, "NUM CORESSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS = default 1 ");
            e.printStackTrace();

            //Default to return 1 core
            return 1;
        }
    }
}
