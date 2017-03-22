package com.lchli.litehotfix;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dalvik.system.PathClassLoader;

/**
 * Created by lchli on 2017/2/21.
 * <p>
 * 轻量级dex热修复。
 * 1，不支持资源文件更改。
 * 2，不支持manifest文件修改。
 * 3，不支持so库修改。
 * 4，不支持Application类修改（因为此类在hook之前就已经加载）。
 * todo:1,api 版本适配。
 */

public class HotFix {

    public static boolean DEBUG = true;

    private String patchDex;
    private String patchDirectory;
    private String infoFilePath;
    private int currentAppVersion;
    private String dexoptPath;

    private static volatile HotFix fix;


    private HotFix() {

    }

    public static HotFix instance() {
        if (fix != null) {
            return fix;
        }

        synchronized (HotFix.class) {
            if (fix == null) {
                fix = new HotFix();
            }
        }
        return fix;

    }

    private void initSetting(Context base) {
        try {
            currentAppVersion = base.getPackageManager().getPackageInfo(base.getPackageName(), PackageManager.GET_CONFIGURATIONS).versionCode;
            log("parse currentAppVersion=" + currentAppVersion);

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

            File dex = new File(patchDir, "patch.rar");
            patchDex = dex.getAbsolutePath();

            final File dexopt = new File(base.getCacheDir().getParentFile(), "dexopt");
            if (!dexopt.exists()) {
                dexopt.mkdirs();
            }
            dexoptPath = dexopt.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /***
     * should call this on {@code Application#attachBaseContext}
     */
    public void init(Context base) {

        try {
            initSetting(base);

            final File patch = new File(patchDex);
            if (!patch.exists()) {
                log("no patch dex file");
                return;//no patch,so we do not install.
            }
            PatchInfo patchInfo = getPatchInfo();
            if (patchInfo == null || patchInfo.targetAppVersion != currentAppVersion) {
                patch.delete();//delete expired dex.
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

            final PathClassLoader originLoader = (PathClassLoader) field_mClassLoader.get(loadedApk);
            Field field_parent = ClassLoader.class.getDeclaredField("parent");
            field_parent.setAccessible(true);

            //use a proxy to replace originLoader.
            field_mClassLoader.set(loadedApk, new FixClassLoader(originLoader));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hook {@code  findClass} method.
     */
    private class FixClassLoader extends ClassLoader {

        private ClassLoader delegate;
        private Object dexPathList;
        private Method m_findClass;
        private List<Throwable> suppressedExceptions = new ArrayList<>();

        public FixClassLoader(ClassLoader delegate) {
            this.delegate = delegate;
            try {
                Class<?> cls_DexPathList = Class.forName("dalvik.system.DexPathList");
                Constructor<?> cons_DexPathList = cls_DexPathList.getDeclaredConstructor(ClassLoader.class, String.class, String.class, File.class);
                cons_DexPathList.setAccessible(true);

                dexPathList = cons_DexPathList.newInstance(delegate, patchDex, patchDex, new File(dexoptPath));
                /**support mutidex*/
                List<File> additionalClassPathEntries = FixUtils.getMutiDexs(new File(patchDirectory));
                if (Build.VERSION.SDK_INT <= 13) {
                    //ignore.

                } else if (Build.VERSION.SDK_INT < 19) {

                    ReflectUtils.expandFieldArray(dexPathList, "dexElements", FixUtils.makeDexElements14_18(dexPathList,
                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath)));

                } else if (Build.VERSION.SDK_INT >= 19) {

                    ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
                    ReflectUtils.expandFieldArray(dexPathList, "dexElements", FixUtils.makeDexElements19_(dexPathList,
                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath),
                            suppressedExceptions));
                }

                m_findClass = cls_DexPathList.getDeclaredMethod("findClass", String.class, List.class);
                m_findClass.setAccessible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                Class fixedClass = (Class) m_findClass.invoke(dexPathList, name, suppressedExceptions);//load fix class first.
                if (fixedClass != null) {
                    return fixedClass;
                }
                throw new ClassNotFoundException();

            } catch (Exception e) {

                try {
                    Method m_findClass = delegate.getClass().getDeclaredMethod("findClass", String.class);//load original class.
                    m_findClass.setAccessible(true);

                    return (Class<?>) m_findClass.invoke(delegate, name);
                } catch (Exception e2) {
                    throw new ClassNotFoundException(e2.getMessage());
                }
            }

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
    public void installPatch(final String patchDexPath, final int targetAppVersion, final int patchVersion, final InstallPatchCallback cb) {
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


    private boolean installPatchImpl(String patchDexPath, int targetAppVersion, int patchVersion) {
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
            FixUtils.performExtractions(new File(patchDex), new File(patchDirectory));//extract multi dex.

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
        if (DEBUG) {
            System.err.println("[*****************************]" + msg);
        }
    }

    private PatchInfo getPatchInfo() {
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
