package com.lchli.litehotfix;

import android.app.Application;
import android.content.Context;

/**
 * Created by lichenghang on 2017/4/8.
 * 使用ApplicationLike的目的是因为：Application引用的类会在应用启动时就已经加载进内存导致无法热修复这些类。
 */

public abstract class ApplicationLike {

    private Application mApplication;

    public ApplicationLike(Application application) {
        mApplication = application;

    }

    protected void attachBaseContext(Context base) {
    }

    public void onCreate() {
    }


    public Application getApplication() {
        return mApplication;
    }
}
