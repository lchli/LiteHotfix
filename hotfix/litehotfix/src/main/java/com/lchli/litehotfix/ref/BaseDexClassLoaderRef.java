package com.lchli.litehotfix.ref;

import com.lchli.litehotfix.util.ReflectUtils;

import java.lang.reflect.Field;

import dalvik.system.BaseDexClassLoader;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class BaseDexClassLoaderRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("dalvik.system.BaseDexClassLoader");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object pathList(BaseDexClassLoader loader) {
        try {
            Object pathlist = ReflectUtils.findField(clazz, "pathList").get(loader);
            if (pathlist != null) {
                return pathlist;
            }

            Field[] fields = clazz.getDeclaredFields();
            if (fields == null || fields.length <= 0) {
                return null;
            }

            for (Field f : fields) {
                if (DexPathListRef.clazz.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f.get(loader);
                }
            }

            return null;

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }


}
