package net.nologin.meep.ca.util;

import android.os.Debug;
import android.util.Log;

import java.text.DecimalFormat;

public class Utils {

    private static final DecimalFormat df = new DecimalFormat();
    static {
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
    }

    public static final String LOG_TAG = "WolframCA";

    public static void log(String msg){
        Log.d(LOG_TAG,msg);
    }

    public static void log(String msg, Throwable t){
        Log.d(LOG_TAG,msg,t);
    }


    public static String getMemStatus(){

        String nat_allo = df.format((double) Debug.getNativeHeapAllocatedSize() / 1048576.0);
        String nat_avail = df.format((double) Debug.getNativeHeapSize() /1048576.0);
        //String nat_free = df.format((double) Debug.getNativeHeapFreeSize() /1048576.0);

        String mem_alloc = df.format((double) (Runtime.getRuntime().totalMemory() / 1048576));
        String mem_avail = df.format((double) (Runtime.getRuntime().maxMemory() / 1048576));
        //String mem_free = df.format((double) (Runtime.getRuntime().freeMemory() / 1048576));

        return String.format("%s/%sMB, nat(%s/%sMB)",mem_alloc,mem_avail,nat_allo,nat_avail);

    }

    public static boolean isHeapAlmostFull() {

        return (Runtime.getRuntime().totalMemory() / (double)Runtime.getRuntime().maxMemory()) > 0.7;


    }
}
