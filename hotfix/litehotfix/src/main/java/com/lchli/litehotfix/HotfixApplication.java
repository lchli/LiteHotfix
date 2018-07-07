package com.lchli.litehotfix;

import android.app.Application;
import android.content.Context;

import com.lchli.litehotfix.util.ReflectUtils;
import com.lchli.litehotfix.util.ResUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by lichenghang on 2017/4/8.
 */

public class HotfixApplication extends Application {

    private static final String RES_APP_LIKE = "res_app_like";

    private Application mApplicationLike;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });

        HotFix.instance().init(base);

        try {
            String R_string_class_name = base.getPackageName() + ".R$string";
            int id = ResUtil.getResourceId(RES_APP_LIKE, Class.forName(R_string_class_name, false, getClassLoader()));
            if (id < 0) {
                throw new HotFixException("you must declare ApplicationLike class name in your string.xml with key=res_app_like");
            }
            String appLikeClassName = base.getString(id);

            Class appLikeCls = Class.forName(appLikeClassName, false, getClassLoader());
            Constructor structor = ReflectUtils.findConstructor(appLikeCls);
            mApplicationLike = (Application) structor.newInstance();

            Method m_attachBaseContext = ReflectUtils.findMethod(Application.class, "attachBaseContext", Context.class);
            m_attachBaseContext.invoke(mApplicationLike,base);



        } catch (Exception e) {
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


}
