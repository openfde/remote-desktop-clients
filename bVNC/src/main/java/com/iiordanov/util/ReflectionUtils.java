package com.iiordanov.util;

import android.util.Log;

import java.lang.reflect.Method;

public class ReflectionUtils {
    public static final String TAG = "Reflection_Utils";
    
    public static void set(String key, String defaultValue) {
        try {
            final Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            final Method set = systemProperties.getMethod("set", String.class, String.class);
            set.invoke(null, key, defaultValue);
            Log.d(TAG,"set " + key + " " + defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Exception while setting system property: ", e);
        }
    }
}
