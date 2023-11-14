package com.iiordanov.bVNC;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;
import androidx.appcompat.app.AppCompatDelegate;

import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.log.HttpLog;
import com.xwdz.http.log.HttpLoggingInterceptor;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class App extends Application {

    private static final String TAG = "VncApp";
    private Database database;
    private static WeakReference<Context> context;
    public static boolean debugLog = false;
    public Set<String> runningAppAct = new HashSet<>();
    public Set<Activity> runningAct = new HashSet<>();
    public HashMap<String, String> runningApp = new HashMap<>();
    private static App instance;
    private static final String canvasActivityName = "com.iiordanov.bVNC.RemoteCanvasActivity";
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    public static App getApp(){
        return instance;
    }

    public static String generateCanvasActivityName(String name) {
        instance.runningAct.stream().forEach(item->{
//            Log.d(TAG, "generateCanvasActivityName() called with: runningAppAct = [" + item + "]");
        });
        instance.runningAct.stream().forEach(item->{
//            Log.d(TAG, "generateCanvasActivityName() called with: runningAct = [" + item + "]");
        });
//        Log.d(TAG, "generateCanvasActivityName() called with: name = [" + name + "]");
        if(!TextUtils.isEmpty(name)){
            for (Map.Entry entry: instance.runningApp.entrySet()){
                String activity = (String) entry.getKey();
                String app = (String) entry.getValue();
                if(TextUtils.equals(name, app)){
//                    Log.d(TAG, "generateCanvasActivityName() returned: " + activity );
                    return activity;
                }
            }
        }
        if (instance.runningAppAct.size() >= 10) {
            return null;
        } else {
            String activity = canvasActivityName;
            for (int i = 0; i < 10; i++) {
                if (i != 0) {
                    activity = canvasActivityName + "$RemoteCanvasActivity" + i;
                }
                if (!instance.runningAppAct.contains(activity)) {
                    instance.runningApp.put(activity, name);
//                    Log.d(TAG, "generateCanvasActivityName() returned: " + activity );
                    return activity;
                }
            }
        }
        return null;
    }

    public static String getRunName(String activity){
        return instance.runningApp.get(activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Constants.DEFAULT_PROTOCOL_PORT = Utils.getDefaultPort(this);
        database = new Database(this);
        context = new WeakReference<Context>(this);
//        debugLog = BuildConfig.DEBUG; //Utils.querySharedPreferenceBoolean(getApplicationContext(), "moreDebugLoggingTag");
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLog("fde"));
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        String hostip = Utils.getProperty("fusion.vnc.hostip",null);
        Log.d(TAG, "onCreate() called hostip:" + hostip);
        if(!TextUtils.isEmpty(hostip)){
            com.ft.fdevnc.Constants.BASIP = hostip;
            com.ft.fdevnc.Constants.BASEURL = "http://" + com.ft.fdevnc.Constants.BASIP + ":18080";
        }
        OkHttpClient sOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logInterceptor)
                .writeTimeout(10, TimeUnit.SECONDS).build();
        QuietOkHttp.setOkHttpClient(sOkHttpClient);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if(activity.getClass().getName().contains("RemoteCanvasActivity")){
                    runningAppAct.add(activity.getClass().getName());
                }
                runningAct.add(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if(activity.getClass().getName().contains("RemoteCanvasActivity")){
                    runningAppAct.add(activity.getClass().getName());
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if(activity.getClass().getName().contains("RemoteCanvasActivity")){
                    runningAppAct.add(activity.getClass().getName());
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
//                if(activity.getClass().getName().contains("RemoteCanvasActivity")){
//                    runningAppAct.remove(activity.getClass().getName());
//                }
//                runningAct.remove(activity);
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if(activity.getClass().getName().contains("RemoteCanvasActivity")){
                    runningAppAct.remove(activity.getClass().getName());
                }
                runningAct.remove(activity);
            }
        });
        runningAppAct.clear();
    }

    public static boolean isRunning(String activityName){
        return instance.runningAct.stream().anyMatch(activity -> activity.getClass().getName().contains(activityName));
    }

    public static void movetoBack(String activityName){
        instance.runningAct.stream().forEach(activity -> {
            if(activity.getClass().getName().contains(activityName)){
                activity.moveTaskToBack(true);
            }
        });
    }

    public Database getDatabase() {
        return database;
    }

    public static Context getContext() {
        return context.get();
    }
}
