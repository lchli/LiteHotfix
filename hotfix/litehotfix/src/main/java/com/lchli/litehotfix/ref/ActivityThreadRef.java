package com.lchli.litehotfix.ref;

import android.content.Context;
import android.support.annotation.Nullable;

import com.lchli.litehotfix.util.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class ActivityThreadRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("android.app.ActivityThread");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Object currentActivityThread() throws Exception {
        Method method_currentActivityThread = ReflectUtils.findMethod(clazz, "currentActivityThread");
        return method_currentActivityThread.invoke(null);
    }

    public static Map mPackages() throws Exception {
        return (Map) ReflectUtils.findField(clazz, "mPackages").get(currentActivityThread());
    }

    @Nullable
    public static Object getActivityThread(@Nullable Context context) {
        try {
            Object currentActivityThread = ActivityThreadRef.currentActivityThread();

            if (currentActivityThread == null && context != null) {
                // In older versions of Android (prior to frameworks/base 66a017b63461a22842)
                // the currentActivityThread was built on thread locals, so we'll need to try
                // even harder
                Field mLoadedApk = context.getClass().getField("mLoadedApk");
                mLoadedApk.setAccessible(true);
                Object apk = mLoadedApk.get(context);

                Field mActivityThreadField = apk.getClass().getDeclaredField("mActivityThread");
                mActivityThreadField.setAccessible(true);

                currentActivityThread = mActivityThreadField.get(apk);
            }

            return currentActivityThread;

        } catch (Throwable ignore) {
            ignore.printStackTrace();
            return null;
        }

    }


}
