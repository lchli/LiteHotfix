package com.lchli.litehotfix.refs;

import com.lchli.litehotfix.ReflectUtils;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class LoadedApkRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("android.app.LoadedApk");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ClassLoader mClassLoader(Object loadedApk) throws Exception {
        return (ClassLoader) ReflectUtils.findField(clazz, "mClassLoader").get(loadedApk);
    }

    public static void set_mClassLoader(Object loadedApk, ClassLoader classLoader) throws Exception {
        ReflectUtils.findField(clazz, "mClassLoader").set(loadedApk, classLoader);
    }
}
