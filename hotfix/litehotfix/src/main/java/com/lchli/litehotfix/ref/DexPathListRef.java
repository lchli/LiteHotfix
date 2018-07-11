package com.lchli.litehotfix.ref;

import android.annotation.TargetApi;
import android.util.Log;

import com.lchli.litehotfix.util.ReflectUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

    public static Object[] dexElements(Object dexPathList) {
        try {
            Object[] dexElements = (Object[]) ReflectUtils.findField(clazz, "dexElements").get(dexPathList);
            if (dexElements != null) {
                return dexElements;
            }

            Field f = findDexElementsField(dexPathList);
            if (f == null) {
                return null;
            }

            return (Object[]) f.get(dexPathList);

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Field findDexElementsField(Object dexPathList) {
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                Object mayDexElements = f.get(dexPathList);
                if (isDexElements(mayDexElements)) {
                    return f;
                }

            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    private static boolean isDexElements(Object mayDexElements) {
        if (!(mayDexElements instanceof Object[])) {
            return false;
        }
        Object[] may = (Object[]) mayDexElements;
        if (may.length <= 0) {
            return false;
        }
        Object first = may[0];
        if (first == null) {
            return false;
        }

        if (ElementRef.clazz.isAssignableFrom(first.getClass())) {
            return true;
        }

        return false;
    }

    public static boolean set_dexElements(Object dexPathList, Object[] dexElements) {
        try {
            Field f = ReflectUtils.findField(clazz, "dexElements");
            if (f == null) {
                f = findDexElementsField(dexPathList);
            }
            if (f == null) {
                return false;
            }
            f.setAccessible(true);
            f.set(dexPathList, dexElements);

            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
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
            } catch (Throwable ee) {
                return makeDexElements14_18(files, optimizedDirectory);
            }
        }
    }

    private static Method findMakeDexElementsMethod() {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method f : methods) {
                if (f.getName().equals("makeDexElements")) {
                    return f;
                }

            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Object[] makeDexElementsAll(List<File> dexfiles, File optimizedDirectory) {
        try {
            Method m = findMakeDexElementsMethod();
            if (m == null) {
                return null;
            }
            m.setAccessible(true);

            Class<?>[] ptypes = m.getParameterTypes();
            Log.e("dex", ptypes.toString());

            List<Object> params = new ArrayList<>();

            for (Class<?> pc : ptypes) {
                if (List.class.isAssignableFrom(pc))
                    params.add(dexfiles);
                else if (File.class.isAssignableFrom(pc))
                    params.add(optimizedDirectory);
                else
                    params.add(null);
            }

            return (Object[]) m.invoke(null, params.toArray());

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;

    }

    public static boolean addDexPath(Object dexPathList, String dexPath, File optimizedDirectory) {
        try {
            Method m = ReflectUtils.findMethod(clazz, "addDexPath", String.class, File.class);
            if (m == null) {
                return false;
            }
            m.invoke(dexPathList, dexPath, optimizedDirectory);

            return true;

        } catch (Throwable e) {
            e.printStackTrace();
            return false;
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
