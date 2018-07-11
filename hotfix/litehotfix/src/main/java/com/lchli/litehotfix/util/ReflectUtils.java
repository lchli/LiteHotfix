package com.lchli.litehotfix.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static android.R.attr.name;

/**
 * Created by lichenghang on 2017/3/22.
 */

public class ReflectUtils {

    public static Field findField(Class clazz, String name) {

        while (clazz != null) {
            try {
                Field e = clazz.getDeclaredField(name);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }
                return e;
            } catch (NoSuchFieldException var4) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }

    public static Method findMethod(Class clazz, String name, Class... parameterTypes) throws NoSuchMethodException {

        while (clazz != null) {
            try {
                Method e = clazz.getDeclaredMethod(name, parameterTypes);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }
                return e;
            } catch (NoSuchMethodException var5) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList(parameterTypes) + " not found in " + clazz);
    }

    public static Constructor findConstructor(Class clazz, Class... parameterTypes) throws NoSuchMethodException {

        while (clazz != null) {
            try {
                Constructor e = clazz.getDeclaredConstructor(parameterTypes);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }
                return e;
            } catch (NoSuchMethodException var5) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Constructor " + name + " with parameters " + Arrays.asList(parameterTypes) + " not found in " + clazz);
    }


    public static Object[] expandFieldArray(Object[] original, Object[] extraElements) {

        Object[] combined = (Object[]) Array.newInstance(

                original.getClass().getComponentType(), original.length + extraElements.length);

       // System.arraycopy(original, 0, combined, 0, original.length);
        System.arraycopy(extraElements, 0, combined, 0, extraElements.length);

       // System.arraycopy(extraElements, 0, combined, original.length, extraElements.length);
        System.arraycopy(original, 0, combined, extraElements.length, original.length);

        return combined;


    }
}
