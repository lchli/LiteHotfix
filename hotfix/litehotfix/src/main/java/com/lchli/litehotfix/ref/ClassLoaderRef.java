package com.lchli.litehotfix.ref;

import com.lchli.litehotfix.util.ReflectUtils;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class ClassLoaderRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("java.lang.ClassLoader");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Class findClass(ClassLoader instance, String name) throws Exception {

        return (Class) ReflectUtils.findMethod(clazz, "findClass", String.class).invoke(instance, name);
    }
}
