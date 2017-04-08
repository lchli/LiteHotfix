package com.lchli.litehotfix.util;

import java.lang.reflect.Field;

/**
 * Created by lichenghang on 2017/4/8.
 */

public class ResUtil {

    public static int getResourceId(String resName, Class resClass) {
        Class mipmap = resClass;
        try {
            Field field = mipmap.getField(resName);
            int resId = field.getInt(resName);
            return resId;
        } catch (NoSuchFieldException e) {
            return -1;
        } catch (IllegalAccessException e) {
            return -1;
        }

    }
}
