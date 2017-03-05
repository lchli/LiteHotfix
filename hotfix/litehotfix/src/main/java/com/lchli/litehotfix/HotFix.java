package com.lchli.litehotfix;

import android.content.Context;
import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import dalvik.system.PathClassLoader;

/**
 * Created by lchli on 2017/2/21.
 * 需要考虑新apk覆盖安装的情况。
 */

public class HotFix {

    private static String patchDex;
    private static String patchDirectory;
    private static final String P = "p.properties";

    public static void init(Context base) {
        try {
            File patchDir = new File(base.getCacheDir().getParentFile(), "patch");
            if (!patchDir.exists()) {
                patchDir.mkdirs();
            }
            patchDirectory = patchDir.getAbsolutePath();

            File dex = new File(patchDir, "patch.dex");
            if (!dex.exists()) {
                dex.createNewFile();//create a empty dex.
            }
            patchDex = dex.getAbsolutePath();
//change class loader,s parent loader.
            Class<?> class_ActivityThread = Class.forName("android.app.ActivityThread");
            Method method_currentActivityThread = class_ActivityThread.getDeclaredMethod("currentActivityThread");
            method_currentActivityThread.setAccessible(true);
            Object currentActivityThread = method_currentActivityThread.invoke(null);

            final Class<?> cls = currentActivityThread.getClass();
            Field field_mPackages = cls.getDeclaredField("mPackages");
            field_mPackages.setAccessible(true);
            Object mPackagesObj = field_mPackages.get(currentActivityThread);

            Method method_get = mPackagesObj.getClass().getDeclaredMethod("get", Object.class);
            method_get.setAccessible(true);
            final WeakReference loadedApkRef = (WeakReference) method_get.invoke(mPackagesObj, base.getPackageName());
            Object loadedApk = loadedApkRef.get();

            Field field_mClassLoader = loadedApk.getClass().getDeclaredField("mClassLoader");
            field_mClassLoader.setAccessible(true);

            PathClassLoader originLoader = (PathClassLoader) field_mClassLoader.get(loadedApk);
            Field field_parent = ClassLoader.class.getDeclaredField("parent");
            field_parent.setAccessible(true);


            File dexopt = new File(base.getCacheDir().getParentFile(), "dexopt");
            if (!dexopt.exists()) {
                dexopt.mkdirs();
            }
            PathClassLoader loader = new PathClassLoader(patchDex, base.getClassLoader().getParent());
            field_parent.set(originLoader, loader);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * copy pathDex to data dir,if already exists one,it will be override.
     *
     * @param patchDexPath
     * @return
     */
    public static boolean installPatch(String patchDexPath, int targetAppVersion, int pathVersion) {
        if (patchDexPath == null) {
            throw new IllegalArgumentException("patchDexPath can not be null.");
        }
        try {
            File p = new File(patchDirectory, P);
            if (!p.exists()) {
                p.createNewFile();
            }

            FileReader fileReader = new FileReader(p);
            FileWriter fileWriter = new FileWriter(p);

            Properties properties = new Properties();
            properties.load(fileReader);

            String appv = properties.getProperty("targetAppVersion");
            String pv = properties.getProperty("pathVersion");


            properties.setProperty("targetAppVersion", targetAppVersion + "");
            properties.setProperty("pathVersion", pathVersion + "");
            properties.store(fileWriter, "");

            fileReader.close();
            fileWriter.close();

            FileUtils.copyFile(new File(patchDexPath), new File(patchDex));

            log("---------------install patch success,will restart process.......");
            System.exit(0);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    private static void log(String msg) {
        System.err.println(msg);
    }
}
