package com.lchli.litehotfix;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.lchli.litehotfix.ref.BaseDexClassLoaderRef;
import com.lchli.litehotfix.ref.ClassLoaderRef;
import com.lchli.litehotfix.ref.DexPathListRef;
import com.lchli.litehotfix.util.ReflectUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

/**
 * Created by lchli on 2017/2/21.
 * <p>
 * 轻量级热修复。
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

            File dex = new File(patchDir, "patch.dex");

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
            Log.e("dexs", patchDex + "");

//            Map mPackages = ActivityThreadRef.mPackages();
//            final WeakReference loadedApkRef = (WeakReference) mPackages.get(base.getPackageName());
//            Object loadedApk = loadedApkRef.get();

            fixByDexElements();
            //fixByPathlist(base);
            //fixByNewClassloader(base);

            //  final PathClassLoader originLoader = (PathClassLoader) LoadedApkRef.mClassLoader(loadedApk);

            // IncrementalClassLoader.inject(HotFix.class.getClassLoader(), nativeLibraryDir, dexoptPath, Arrays.asList(patchDex, sourceDir));


            //DexClassLoader fixClassLoader= new MyDexClassLoader(patchDex,dexoptPath,null,originLoaderParent,originLoader);

            // MyDexClassLoader  f=new MyDexClassLoader(originLoader);
            //setParent(originLoader,fixClassLoader);


            //use a proxy to replace originLoader.
            //LoadedApkRef.set_mClassLoader(loadedApk, originLoader);
            log("set_mClassLoader success====================================");

            //below:hook resource
//            AssetManager asset = base.getAssets();
//
//            ArrayList<String> oldpaths = new ArrayList<>();
//
//            int blockCount = AssertManagerRef.getStringBlockCount(asset);
//            log("blockCount:" + blockCount);
//
//
//            for (int i = 0; i < blockCount; ++i) {
//                String cookiePath = AssertManagerRef.getCookieName(asset, i + 1);
//                if (cookiePath != null && cookiePath.startsWith("/system/framework/")) {
//                    oldpaths.add(cookiePath);
//                }
//            }
//
//
//            AssertManagerRef.destroy(asset);
//
//            AssertManagerRef.init(asset, false);
//            AssertManagerRef.set_mStringBlocks(asset, null);
//
//            for (String p : oldpaths) {
//                log("system res path:" + p);
//                AssertManagerRef.addAssetPath(asset, p);
//            }
//            AssertManagerRef.addAssetPath(asset, patchDex);
//
//            AssertManagerRef.ensureStringBlocks(asset);
//
//
//            Resources resources = base.getResources();
//            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean fixByDexElements() {
        ClassLoader originalClassLoader = HotFix.class.getClassLoader();
        if (!(originalClassLoader instanceof BaseDexClassLoader)) {
            log("originalClassLoader is not a BaseDexClassLoader.skip fix===");
            return true;
        }
        final BaseDexClassLoader dexOriginalClassLoader = (BaseDexClassLoader) originalClassLoader;

        Object pathList = BaseDexClassLoaderRef.pathList(dexOriginalClassLoader);
        if (pathList == null) {
            log("originalClassLoader's pathList is null.skip fix===");
            return true;
        }

        boolean success = DexPathListRef.addDexPath(pathList, patchDex, new File(dexoptPath));
        if (!success) {
            log("addDexPath fail.skip fix===");
            return true;
        }

        Object[] dexElements = DexPathListRef.dexElements(pathList);
        if (dexElements == null) {
            log("originalClassLoader's dexElements is null.skip fix===");
            return true;
        }

        ArrayUtils.reverse(dexElements);

//        ArrayList<File> fixDexFiles = new ArrayList<File>();
//        fixDexFiles.add(new File(patchDex));
//
//        Object[] newDexElems = DexPathListRef.makeDexElementsAll(
//                fixDexFiles, new File(dexoptPath));
//        if (newDexElems == null) {
//            log("make newDexElems is null.skip fix===");
//            return true;
//        }
//
//        Object[] combined = ReflectUtils.expandFieldArray(dexElements, newDexElems);
//
//        DexPathListRef.set_dexElements(pathList, combined);

        return false;
    }

    private void fixByNewClassloader(Context base) {
        String sourceDir = base.getApplicationInfo().sourceDir;
        String nativeLibraryDir = base.getApplicationInfo().nativeLibraryDir;

        ClassLoader originalClassLoader = HotFix.class.getClassLoader();
        ClassLoader systemClassLoader = originalClassLoader.getParent();

        String pathBuilder = createDexPath(Arrays.asList(patchDex, sourceDir));
        BaseDexClassLoader fixClassLoader = new BaseDexClassLoader(pathBuilder, new File(dexoptPath),
                nativeLibraryDir, systemClassLoader);

        setParent(originalClassLoader, fixClassLoader);
    }

    private void fixByPathlist(Context base) throws Exception {//not good.
        String sourceDir = base.getApplicationInfo().sourceDir;
        String nativeLibraryDir = base.getApplicationInfo().nativeLibraryDir;

        ClassLoader originalClassLoader = HotFix.class.getClassLoader();

        Class[] parameterTypes = new Class[]{ClassLoader.class, String.class, String.class, File.class};

        Object dexPathList = DexPathListRef.newInstance(parameterTypes, originalClassLoader,
                createDexPath(Arrays.asList(patchDex, sourceDir)),
                nativeLibraryDir, new File(dexoptPath));

        Field f_pathList = ReflectUtils.findField(originalClassLoader.getClass(), "pathList");

        f_pathList.set(originalClassLoader, dexPathList);
    }


    private static String createDexPath(List<String> dexes) {
        StringBuilder pathBuilder = new StringBuilder();
        boolean first = true;
        for (String dex : dexes) {
            if (first) {
                first = false;
            } else {
                pathBuilder.append(File.pathSeparator);
            }

            pathBuilder.append(dex);
        }


        Log.v("hotfix", "Incremental dex path is "
                + pathBuilder.toString());

        return pathBuilder.toString();
    }

    private static void setParent(ClassLoader classLoader, ClassLoader newParent) {
        try {
            Field parent = ClassLoader.class.getDeclaredField("parent");
            parent.setAccessible(true);
            parent.set(classLoader, newParent);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    private class MyDexClassLoader extends DexClassLoader {//orign->mydex(bootparent)->orign

        private ClassLoader originLoader;

        public MyDexClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent,
                                ClassLoader originLoader) {
            super(dexPath, optimizedDirectory, librarySearchPath, parent);
            this.originLoader = originLoader;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return super.findClass(name);
            } catch (Throwable e) {
//                try {
//                    return ClassLoaderRef.findClass(originLoader, name);
//                } catch (Exception e2) {
//                    //  e2.printStackTrace();
//                    throw new ClassNotFoundException(e2.getMessage());
//                }

                throw new ClassNotFoundException(e.getMessage());
            }


        }
    }

    /**
     * hook {@code  findClass} method.
     */
    private class FixClassLoader extends ClassLoader {

        private ClassLoader originloader;
        private ClassLoader originloaderParent;
        private ClassLoader myloader;
        private Object dexPathList;
        private List<Throwable> suppressedExceptions = new ArrayList<>();

        public FixClassLoader(ClassLoader delegate) {
            this.originloader = delegate;
            this.originloaderParent = originloader.getParent();
            try {

                myloader = new DexClassLoader(patchDex, dexoptPath, null, originloaderParent);

                // Class[] parameterTypes = new Class[]{ClassLoader.class, String.class, String.class, File.class};
                //dexPathList = DexPathListRef.newInstance(parameterTypes, delegate, patchDex, patchDex, new File(dexoptPath));
                /**support mutidex*/
//                List<File> additionalClassPathEntries = FixUtils.getMutiDexs(new File(patchDirectory));
//                Object[] combined = null;
//
//                if (Build.VERSION.SDK_INT <= 13) {
//                    //ignore.
//
//                } else if (Build.VERSION.SDK_INT < 19) {
//
//                    combined = ReflectUtils.expandFieldArray(DexPathListRef.dexElements(dexPathList), DexPathListRef.makeDexElements14_18(
//                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath)));
//
//                } else if (Build.VERSION.SDK_INT <= 23) {
//
//                    ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
//
//                    combined = ReflectUtils.expandFieldArray(DexPathListRef.dexElements(dexPathList), DexPathListRef.makeDexElements19_23(
//                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath),
//                            suppressedExceptions));
//                } else {//7.0
//                    ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
//
//                    combined = ReflectUtils.expandFieldArray(DexPathListRef.dexElements(dexPathList), DexPathListRef.makeDexElements24_(
//                            new ArrayList<File>(additionalClassPathEntries), new File(dexoptPath),
//                            suppressedExceptions, originloader));
//                }
//
//                if (combined != null) {
//                    DexPathListRef.set_dexElements(dexPathList, combined);
//                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                Log.d("hotfix", "findClass=================================================:" + name);

                // Class fixedClass = DexPathListRef.findClass(dexPathList, name, suppressedExceptions);//load fix class first.
                Class fixedClass = ClassLoaderRef.findClass(myloader, name);//load fix class first.
                if (fixedClass != null) {
                    Log.e("hotfix", "fixedClass=================================================:" + name);
                    return fixedClass;
                }
                Log.e("hotfix", "fixedClass not found=================================================:" + name);

                throw new ClassNotFoundException();

            } catch (Exception e) {
                //if(name.startsWith("com.lch.menote.note.ui.LocalNoteListAdp"))
                e.printStackTrace();

                try {
                    return ClassLoaderRef.findClass(originloader, name);
                } catch (Exception e2) {
                    //  e2.printStackTrace();
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
            File patchDexFile = new File(patchDex);
            if (patchDexFile.exists()) {
                patchDexFile.delete();
            }

            File dexopt = new File(dexoptPath);
            FileUtils.deleteDirectory(dexopt);

            FileUtils.copyFile(new File(patchDexPath), patchDexFile);
            // FixUtils.performExtractions(new File(patchDex), new File(patchDirectory));//extract multi dex.

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
