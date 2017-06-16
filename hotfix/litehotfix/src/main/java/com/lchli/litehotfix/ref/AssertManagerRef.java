package com.lchli.litehotfix.ref;

import android.content.res.AssetManager;

import com.lchli.litehotfix.util.ReflectUtils;

/**
 * Created by lichenghang on 2017/4/1.
 */

public class AssertManagerRef {

    public static Class<?> clazz;

    static {
        try {
            clazz = Class.forName("android.content.res.AssetManager");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init(AssetManager asset, boolean isSystem) throws Exception {
        ReflectUtils.findMethod(clazz, "init", Boolean.TYPE).invoke(asset, isSystem);
    }

    public static void destroy(AssetManager asset) throws Exception {
        ReflectUtils.findMethod(clazz, "destroy").invoke(asset);
    }


    public static int addAssetPath(AssetManager asset, String path) throws Exception {
        return (int) ReflectUtils.findMethod(clazz, "addAssetPath", String.class).invoke(asset, path);
    }


    public static Object ensureStringBlocks(AssetManager asset) throws Exception {
        return ReflectUtils.findMethod(clazz, "ensureStringBlocks").invoke(asset);
    }


    public static void set_mStringBlocks(AssetManager asset, Object stringBlocks) throws Exception {
        ReflectUtils.findField(clazz, "mStringBlocks").set(asset, stringBlocks);
    }

    public static int getStringBlockCount(AssetManager asset) throws Exception {
        return (int) ReflectUtils.findMethod(clazz, "getStringBlockCount").invoke(asset);
    }

    public static String getCookieName(AssetManager asset, int cookie) throws Exception {
        return (String) ReflectUtils.findMethod(clazz, "getCookieName", int.class).invoke(asset, cookie);
    }

}
