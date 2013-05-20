package net.nologin.meep.ca.util;

import android.content.Context;
import android.content.SharedPreferences;
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
}
