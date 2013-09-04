/*
 *    WolframCA - an android application to view 1-dimensional cellular automata (CA)
 *    Copyright 2013 Barry O'Neill (http://barryoneill.net/)
 *
 *    Licensed under Apache 2.0 with limited permission from, and no affiliation with Steven
 *    Wolfram, LLC. See the LICENSE file in the root of this project for the full license terms.
 */
package net.nologin.meep.ca;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Static utility methods for the wolfram app that don't really belong anywhere else.
 */
public class WolframUtils {

    /**
     * Log tag for this app, all messages have the tag "WolframCA"
     */
    public static final String LOG_TAG = "WolframCA";

    private WolframUtils() {
    } // no instantiation

    /**
     * Preference related stuff only here
     */
    public static class Prefs {

        private static SharedPreferences getPrefs(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx);
        }


        /**
         * @param ctx context
         * @return <code>true</code> if the 'show debug' preference is enabled
         */
        public static boolean getPrefDebugEnabled(Context ctx) {
            return getPrefs(ctx).getBoolean(SettingsActivity.PREF_KEY_SHOW_DEBUG, false);
        }
    }

    /**
     * Sanitize the provided zoom level
     *
     * @param val The value
     * @return If the value lies outsize the range 1-16, it will be set to 1 or 16 (whichever is closest).  Within
     *         that range, the value will rounded to the nearest multiple of 2 (eg 2, 4, 6,..).
     */
    public static int sanitizeZoom(int val) {

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

    /**
     * Return the <code>android:versionName</code> as defined in the manifest
     *
     * @param ctx The context
     * @return The name as reported by the context's packagemanager.
     */
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
     * <a href="http://stackoverflow.com/a/10377934/276183">http://stackoverflow.com/a/10377934/276183</a>
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

            Log.e(WolframUtils.LOG_TAG, "NUM CORESSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS " + files.length);
            return files.length;
        } catch (Exception e) {

            Log.e(WolframUtils.LOG_TAG, "NUM CORESSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS = default 1 ");
            e.printStackTrace();

            //Default to return 1 core
            return 1;
        }
    }




}
