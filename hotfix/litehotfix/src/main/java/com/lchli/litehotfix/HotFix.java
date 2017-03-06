package com.lchli.litehotfix;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import dalvik.system.PathClassLoader;

/**
 * Created by lchli on 2017/2/21.
 */

public class HotFix {

    private static String patchDex;
    private static String patchDirectory;
    private static String infoFilePath;

    private static int currentAppVersion = 0;

    /***
     * should call this on {@code Application#attachBaseContext}
     *
     * @param base
     */
    public static void init(Context base) {

        try {
            currentAppVersion = base.getPackageManager().getPackageInfo(base.getPackageName(), PackageManager.GET_CONFIGURATIONS).versionCode;

            File patchDir = new File(base.getCacheDir().getParentFile(), "patch");
            if (!patchDir.exists()) {
                patchDir.mkdirs();
            }
            patchDirectory = patchDir.getAbsolutePath();

            File info = new File(patchDirectory, "p.properties");
            if (!info.exists()) {
                info.createNewFile();
            }
            infoFilePath = info.getAbsolutePath();

            File dex = new File(patchDir, "patch.dex");
            patchDex = dex.getAbsolutePath();
            if (!dex.exists()) {
                log("no patch dex file");
                return;//no patch,so we do not install.
            }
            PatchInfo patchInfo = getPatchInfo();
            if (patchInfo == null || patchInfo.targetAppVersion != currentAppVersion) {
                dex.delete();//delete expired dex.
                log("patch dex file is expired.");
                return;
            }

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

    /***
     * copy pathDex to data dir,if already exists one,it will be override.
     *
     * @param patchDexPath
     * @param targetAppVersion
     * @param patchVersion
     * @param cb
     */
    public static void installPatch(final String patchDexPath, final int targetAppVersion, final int patchVersion, final InstallPatchCallback cb) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return installPatchImpl(patchDexPath, targetAppVersion, patchVersion);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                cb.onFinish(aBoolean);
            }
        }.execute();

    }


    private static boolean installPatchImpl(String patchDexPath, int targetAppVersion, int patchVersion) {
        if (patchDexPath == null) {
            throw new IllegalArgumentException("patchDexPath can not be null.");
        }
        if (targetAppVersion != currentAppVersion) {
            log("targetAppVersion not match.");
            return false;
        }
        PatchInfo patchInfo = getPatchInfo();
        if (patchInfo != null && patchInfo.patchVersion == patchVersion) {
            log("patch already installed.");
            return false;//already installed.
        }

        File p = new File(infoFilePath);
        FileWriter fileWriter = null;

        try {
            FileUtils.copyFile(new File(patchDexPath), new File(patchDex));

            fileWriter = new FileWriter(p);
            Properties properties = new Properties();
            properties.setProperty(PatchInfo.field_targetAppVersion, targetAppVersion + "");
            properties.setProperty(PatchInfo.field_patchVersion, patchVersion + "");
            properties.store(fileWriter, "");

            log("---------------install patch success,please restart process.......");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void log(String msg) {
        System.err.println("[*****************************]" + msg);
    }

    private static PatchInfo getPatchInfo() {
        File info = new File(infoFilePath);
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(info);

            Properties properties = new Properties();
            properties.load(fileReader);

            String dexTarget = properties.getProperty(PatchInfo.field_targetAppVersion);
            if (TextUtils.isEmpty(dexTarget)) {
                return null;
            }
            String pathVersion = properties.getProperty(PatchInfo.field_patchVersion);
            if (TextUtils.isEmpty(pathVersion)) {
                return null;
            }
            PatchInfo patchInfo = new PatchInfo();
            patchInfo.targetAppVersion = Integer.parseInt(dexTarget);
            patchInfo.patchVersion = Integer.parseInt(pathVersion);

            return patchInfo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private static class PatchInfo {

        private int targetAppVersion;
        private int patchVersion;

        private static final String field_targetAppVersion = "targetAppVersion";
        private static final String field_patchVersion = "patchVersion";
    }

    public interface InstallPatchCallback {

        void onFinish(boolean isSuccess);
    }

}
