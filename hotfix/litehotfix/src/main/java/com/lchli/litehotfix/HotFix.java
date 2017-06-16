package com.lchli.litehotfix;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.lchli.litehotfix.ref.ActivityThreadRef;
import com.lchli.litehotfix.ref.AssertManagerRef;
import com.lchli.litehotfix.ref.ClassLoaderRef;
import com.lchli.litehotfix.ref.DexPathListRef;
import com.lchli.litehotfix.ref.LoadedApkRef;
import com.lchli.litehotfix.util.FixUtils;
import com.lchli.litehotfix.util.ReflectUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dalvik.system.PathClassLoader;

/**
 * Created by lchli on 2017/2/21.
 * <p>
 * 轻量级dex热修复。
 * 1，不支持资源文件更改。
 * 2，不支持manifest文件修改。
 * 3，不支持so库修改。
 * fix proguard use :--applyMapping oldmaping.txt
 * in proguard.txt
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

            Map mPackages = ActivityThreadRef.mPackages();
            final WeakReference loadedApkRef = (WeakReference) mPackages.get(base.getPackageName());
            Object loadedApk = loadedApkRef.get();

            final PathClassLoader originLoader = (PathClassLoader) LoadedApkRef.mClassLoader(loadedApk);
            //use a proxy to replace originLoader.
            LoadedApkRef.set_mClassLoader(loadedApk, new FixClassLoader(originLoader));


            //below:hook resource
            AssetManager asset = base.getAssets();

            ArrayList<String> oldpaths = new ArrayList<>();

            int blockCount = AssertManagerRef.getStringBlockCount(asset);
            log("blockCount:" + blockCount);


            for (int i = 0; i < blockCount; ++i) {
                String cookiePath = AssertManagerRef.getCookieName(asset, i + 1);
                if (cookiePath != null && cookiePath.startsWith("/system/framework/")) {
                    oldpaths.add(cookiePath);
                }
            }


            AssertManagerRef.destroy(asset);

            AssertManagerRef.init(asset, false);
            AssertManagerRef.set_mStringBlocks(asset, null);

            for (String p : oldpaths) {
                log("system res path:" + p);
                AssertManagerRef.addAssetPath(asset, p);
            }
            AssertManagerRef.addAssetPath(asset, patchDex);

            AssertManagerRef.ensureStringBlocks(asset);


            Resources resources = base.getResources();
            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());


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
        private List<Throwable> suppressedExceptions = new ArrayList<>();

        public FixClassLoader(ClassLoader delegate) {
            this.delegate = delegate;
            try {

                Class[] parameterTypes = new Class[]{ClassLoader.class, String.class, String.class, File.class};
                dexPathList = DexPathListRef.newInstance(parameterTypes, delegate, patchDex, patchDex, new File(dexoptPath));
                /**support mutidex*/
                List<File> additionalClassPathEntries = FixUtils.getMutiDexs(new File(patchDirectory));
                Object[] combined = null;

                if (Build.VERSION.SDK_INT <= 13) {
                    //ignore.

                } else if (Build.VERSION.SDK_INT < 19) {

                    combined = ReflectUtils.expandFieldArray(DexPathListRef.dexElements(dexPathList), DexPathListRef.makeDexElements14_18(
                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath)));

                } else if (Build.VERSION.SDK_INT <= 23) {

                    ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();

                    combined = ReflectUtils.expandFieldArray(DexPathListRef.dexElements(dexPathList), DexPathListRef.makeDexElements19_23(
                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath),
                            suppressedExceptions));
                } else {//7.0
                    ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();

                    combined = ReflectUtils.expandFieldArray(DexPathListRef.dexElements(dexPathList), DexPathListRef.makeDexElements24_(
                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath),
                            suppressedExceptions, delegate));
                }

                if (combined != null) {
                    DexPathListRef.set_dexElements(dexPathList, combined);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                Class fixedClass = DexPathListRef.findClass(dexPathList, name, suppressedExceptions);//load fix class first.
                if (fixedClass != null) {
                    return fixedClass;
                }
                throw new ClassNotFoundException();

            } catch (Exception e) {

                try {
                    return ClassLoaderRef.findClass(delegate, name);
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
