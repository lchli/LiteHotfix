package com.lchli.litehotfix.ref;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class ElementRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("dalvik.system.DexPathList$Element");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
