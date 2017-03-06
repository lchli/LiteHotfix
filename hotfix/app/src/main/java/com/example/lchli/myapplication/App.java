package com.example.lchli.myapplication;

import android.app.Application;
import android.content.Context;

import com.lchli.litehotfix.HotFix;

/**
 * Created by lchli on 2017/2/20.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });

        HotFix.instance(base).init();

        super.attachBaseContext(base);
    }


}
