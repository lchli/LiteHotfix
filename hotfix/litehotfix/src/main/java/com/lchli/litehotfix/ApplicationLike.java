package com.lchli.litehotfix;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Created by lichenghang on 2017/4/8.
 * 使用ApplicationLike的目的是因为：Application引用的类会在应用启动时就已经加载进内存导致无法热修复这些类。
 */

public abstract class ApplicationLike {

    private Application mApplication;

    public ApplicationLike(Application application) {
        mApplication = application;
    }

    public void attachBaseContext(Context base) {
    }

    public void onCreate() {
    }

    public void onTerminate() {
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public final Application getApplication() {
        return mApplication;
    }
}