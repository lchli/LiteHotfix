package com.lchli.litehotfix.ref;

import android.annotation.TargetApi;

import com.lchli.litehotfix.util.ReflectUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class DexPathListRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("dalvik.system.DexPathList");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object newInstance(Class[] parameterTypes, Object... args) throws Exception {

        return ReflectUtils.findConstructor(clazz, parameterTypes).newInstance(args);
    }

    public static Class findClass(Object dexPathList, String className, List<Throwable> suppressedExceptions) throws Exception {

        return (Class) ReflectUtils.findMethod(clazz, "findClass", String.class, List.class)
                .invoke(dexPathList, className, suppressedExceptions);
    }

    public static Object[] dexElements(Object dexPathList) throws Exception {

        return (Object[]) ReflectUtils.findField(clazz, "dexElements").get(dexPathList);
    }

    public static void set_dexElements(Object dexPathList, Object[] dexElements) throws Exception {

        ReflectUtils.findField(clazz, "dexElements").set(dexPathList, dexElements);
    }

    @TargetApi(14)
    public static Object[] makeDexElements14_18(ArrayList<File> files, File optimizedDirectory) throws Exception {

        return (Object[]) ReflectUtils.findMethod(clazz, "makeDexElements", ArrayList.class, File.class)
                .invoke(null, files, optimizedDirectory);
    }

    @TargetApi(19)
    public static Object[] makeDexElements19_23(ArrayList<File> files, File optimizedDirectory, ArrayList<IOException> suppressedExceptions) throws Exception {

        return (Object[]) ReflectUtils.findMethod(clazz, "makeDexElements", ArrayList.class, File.class, ArrayList.class)
                .invoke(null, files, optimizedDirectory, suppressedExceptions);
    }

    @TargetApi(24)
    public static Object[] makeDexElements24_(ArrayList<File> files, File optimizedDirectory, ArrayList<IOException> suppressedExceptions, ClassLoader classLoader) throws Exception {
        try {
            return (Object[]) ReflectUtils.findMethod(clazz, "makeDexElements", ArrayList.class, File.class, ArrayList.class, ClassLoader.class)
                    .invoke(null, files, optimizedDirectory, suppressedExceptions, classLoader);
        } catch (Throwable e) {
            e.printStackTrace();
            try {
                return makeDexElements19_23(files, optimizedDirectory, suppressedExceptions);
            }catch (Throwable ee){
                return makeDexElements14_18(files, optimizedDirectory);
            }
        }
    }

    //for test.
    public static void printClassInfo() {

        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            System.err.println(m.toGenericString());
        }

    }
}
