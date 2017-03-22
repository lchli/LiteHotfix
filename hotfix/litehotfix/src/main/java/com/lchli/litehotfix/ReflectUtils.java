package com.lchli.litehotfix;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by lichenghang on 2017/3/22.
 */

public class ReflectUtils {

    public static Field findField(Object instance, String name) throws NoSuchFieldException {
        Class clazz = instance.getClass();
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
        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    public static Method findMethod(Object instance, String name, Class... parameterTypes) throws NoSuchMethodException {
        Class clazz = instance.getClass();
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
        throw new NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList(parameterTypes) + " not found in " + instance.getClass());
    }


    /**
     * Replace the value of a field containing a non null array, by a new array containing the
     * <p>
     * elements of the original array plus the elements of extraElements.
     *
     * @param instance      the instance whose field is to be modified.
     * @param fieldName     the field to modify.
     * @param extraElements elements to append at the end of the array.
     */
    public static void expandFieldArray(Object instance, String fieldName, Object[] extraElements) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Field jlrField = findField(instance, fieldName);

        Object[] original = (Object[]) jlrField.get(instance);

        Object[] combined = (Object[]) Array.newInstance(

                original.getClass().getComponentType(), original.length + extraElements.length);

        System.arraycopy(original, 0, combined, 0, original.length);

        System.arraycopy(extraElements, 0, combined, original.length, extraElements.length);

        jlrField.set(instance, combined);

    }
}
