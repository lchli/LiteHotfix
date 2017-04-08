package com.example.lchli.myapplication;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.lchli.litehotfix.ApplicationLike;

/**
 * Created by lchli on 2017/2/20.
 */

public class AppLike extends ApplicationLike {

    private static final String test = "love fixed";

    public static Handler handler = new Handler();

    public AppLike(Application application) {
        super(application);
    }


    @Override
    protected void attachBaseContext(Context base) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
        super.attachBaseContext(base);

        System.err.println("attachBaseContext=============" + test);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        System.err.println("onCreate");
    }
}
