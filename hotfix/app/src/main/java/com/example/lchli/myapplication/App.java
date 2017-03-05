package com.example.lchli.myapplication;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.lchli.litehotfix.HotFix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

import static android.R.attr.path;

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
        HotFix.init(base);
        super.attachBaseContext(base);
    }


}
