package com.lchli.litehotfix;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import com.lchli.litehotfix.util.ReflectUtils;
import com.lchli.litehotfix.util.ResUtil;

import java.lang.reflect.Constructor;

/**
 * Created by lichenghang on 2017/4/8.
 */

public class HotfixApplication extends Application {

    private static final String RES_APP_LIKE = "baf_hotfix_res_app_like";

    private ApplicationLike mApplicationLike;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        HotFix.instance().init(base);

        callAttachBaseContext(base);
    }

    private void callAttachBaseContext(Context base) {
        try {
            final String R_string_class_name = base.getPackageName() + ".R$string";

            int id = ResUtil.getResourceId(RES_APP_LIKE, Class.forName(R_string_class_name, false, getClassLoader()));
            if (id < 0) {
                throw new HotFixException("you must declare ApplicationLike class name in your string.xml with key=baf_hotfix_res_app_like");
            }
            String appLikeClassName = base.getString(id);
            Log.e("hotfix","appLikeClassName:"+appLikeClassName);


            Class appLikeCls = Class.forName(appLikeClassName, false, getClassLoader());

            Constructor structor = ReflectUtils.findConstructor(appLikeCls, Application.class);
            mApplicationLike = (ApplicationLike) structor.newInstance(this);

            mApplicationLike.attachBaseContext(base);

        } catch (Throwable e) {
            e.printStackTrace();
            throw new HotFixException(e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mApplicationLike != null) {
            mApplicationLike.onCreate();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mApplicationLike != null) {
            mApplicationLike.onLowMemory();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        if (mApplicationLike != null) {
            mApplicationLike.onTerminate();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (mApplicationLike != null) {
            mApplicationLike.onTrimMemory(level);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mApplicationLike != null) {
            mApplicationLike.onConfigurationChanged(newConfig);
        }
    }
}
