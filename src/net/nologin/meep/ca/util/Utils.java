package net.nologin.meep.ca.util;

import android.content.Context;
import android.util.Log;
import net.nologin.meep.ca.R;

public class Utils {

    public static final String LOG_TAG = "WolframCA";

    public static void logD(String msg){
        Log.d(LOG_TAG,msg);
    }

    public static void logD(String msg, Throwable t){
        Log.d(LOG_TAG,msg,t);
    }



}
