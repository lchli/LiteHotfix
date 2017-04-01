package com.example.lchli.myapplication;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.lchli.litehotfix.HotFix;

/**
 * Created by lchli on 2017/2/20.
 */

public class App extends Application {

    public static App app;
    public static Handler handler=new Handler();



    @Override
    protected void attachBaseContext(Context base) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });

        HotFix.instance().init(base);

        super.attachBaseContext(base);

        System.err.println("attachBaseContext");
    }



    @Override
    public void onCreate() {
        super.onCreate();
        app=this;
        System.err.println("onCreate");
    }
}
