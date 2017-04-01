package com.lchli.litehotfix.refs;

import com.lchli.litehotfix.ReflectUtils;

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


    public static Object currentActivityThread() throws Exception {
        Method method_currentActivityThread = ReflectUtils.findMethod(clazz, "currentActivityThread");
        return method_currentActivityThread.invoke(null);
    }

    public static Map mPackages() throws Exception {
        return (Map) ReflectUtils.findField(clazz,"mPackages").get(currentActivityThread());
    }
}
