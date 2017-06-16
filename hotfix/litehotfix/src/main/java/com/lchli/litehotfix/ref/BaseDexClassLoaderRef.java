package com.lchli.litehotfix.ref;

import com.lchli.litehotfix.util.ReflectUtils;

import dalvik.system.BaseDexClassLoader;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class BaseDexClassLoaderRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("dalvik.system.BaseDexClassLoaderRef");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object pathList(BaseDexClassLoader loader) throws Exception {
        return ReflectUtils.findField(clazz, "pathList").get(loader);
    }


}
