package com.iiordanov.bVNC;

import android.content.Context;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.appcompat.app.AppCompatDelegate;

import com.iiordanov.pubkeygenerator.BuildConfig;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.log.HttpLog;
import com.xwdz.http.log.HttpLoggingInterceptor;

import java.lang.ref.WeakReference;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class App extends MultiDexApplication {

    private Database database;
    private static WeakReference<Context> context;
    public static boolean debugLog = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(getBaseContext());

    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Constants.DEFAULT_PROTOCOL_PORT = Utils.getDefaultPort(this);
        database = new Database(this);
        context = new WeakReference<Context>(this);
//        debugLog = BuildConfig.DEBUG; //Utils.querySharedPreferenceBoolean(getApplicationContext(), "moreDebugLoggingTag");
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLog("fde"));
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        OkHttpClient sOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logInterceptor)
                .writeTimeout(10, TimeUnit.SECONDS).build();
        QuietOkHttp.setOkHttpClient(sOkHttpClient);
    }

    public Database getDatabase() {
        return database;
    }

    public static Context getContext() {
        return context.get();
    }
}
