package com.lchli.litehotfix.util;

import android.app.Application;
import android.content.Context;
import android.support.annotation.Nullable;

import com.lchli.litehotfix.ref.ActivityThreadRef;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
// TODO: 2018/7/11  //same with ali jiagu and google instant run.  tinker why not fix application.
// FIXME: 2018/7/13 这种方式修复application会有兼容性问题，并且无法通过开关控制，所以建议使用tinker这种application like的方式。
@Deprecated
public class MonkeyPatcher {

    @SuppressWarnings("unchecked")  // Lots of conversions with generic types
    public static void monkeyPatchApplication(@Nullable Context context,
                                              @Nullable Application bootstrap,
                                              @Nullable Application realApplication
                                             ) {
        /*
        The code seems to perform this:
        Application realApplication = the newly instantiated (in attachBaseContext) user app

        currentActivityThread = ActivityThread.currentActivityThread;
        Application initialApplication = currentActivityThread.mInitialApplication;
        if (initialApplication == BootstrapApplication.this) {
            currentActivityThread.mInitialApplication = realApplication;

        // Replace all instance of the stub application in ActivityThread#mAllApplications with the
        // real one
        List<Application> allApplications = currentActivityThread.mAllApplications;
        for (int i = 0; i < allApplications.size(); i++) {
            if (allApplications.get(i) == BootstrapApplication.this) {
                allApplications.set(i, realApplication);
            }
        }

        // Enumerate all LoadedApk (or PackageInfo) fields in ActivityThread#mPackages and
        // ActivityThread#mResourcePackages and do two things:
        //   - Replace the Application instance in its mApplication field with the real one
        //   - Replace mResDir to point to the external resource file instead of the .apk. This is
        //     used as the asset path for new Resources objects.
        //   - Set Application#mLoadedApk to the found LoadedApk instance

        ArrayMap<String, WeakReference<LoadedApk>> map1 = currentActivityThread.mPackages;
        for (Map.Entry<String, WeakReference<?>> entry : map1.entrySet()) {
            Object loadedApk = entry.getValue().get();
            if (loadedApk == null) {
                continue;
            }

            if (loadedApk.mApplication == BootstrapApplication.this) {
                loadedApk.mApplication = realApplication;
                if (externalResourceFile != null) {
                    loadedApk.mResDir = externalResourceFile;
                }
                realApplication.mLoadedApk = loadedApk;
            }
        }

        // Exactly the same as above, except done for mResourcePackages instead of mPackages
        ArrayMap<String, WeakReference<LoadedApk>> map2 = currentActivityThread.mResourcePackages;
        for (Map.Entry<String, WeakReference<?>> entry : map2.entrySet()) {
            Object loadedApk = entry.getValue().get();
            if (loadedApk == null) {
                continue;
            }

            if (loadedApk.mApplication == BootstrapApplication.this) {
                loadedApk.mApplication = realApplication;
                if (externalResourceFile != null) {
                    loadedApk.mResDir = externalResourceFile;
                }
                realApplication.mLoadedApk = loadedApk;
            }
        }
        */

        // BootstrapApplication is created by reflection in Application#handleBindApplication() ->
        // LoadedApk#makeApplication(), and its return value is used to set the Application field in all
        // sorts of Android internals.
        //
        // Fortunately, Application#onCreate() is called quite soon after, so what we do is monkey
        // patch in the real Application instance in BootstrapApplication#onCreate().
        //
        // A few places directly use the created Application instance (as opposed to the fields it is
        // eventually stored in). Fortunately, it's easy to forward those to the actual real
        // Application class.
        try {
            // Find the ActivityThread instance for the current thread
            final Class<?> activityThread = ActivityThreadRef.clazz;

            Object currentActivityThread = ActivityThreadRef.getActivityThread(context);

            // Find the mInitialApplication field of the ActivityThread to the real application
            Field mInitialApplication = activityThread.getDeclaredField("mInitialApplication");
            mInitialApplication.setAccessible(true);
            Application initialApplication = (Application) mInitialApplication.get(currentActivityThread);

            if (realApplication != null && initialApplication == bootstrap) {
                mInitialApplication.set(currentActivityThread, realApplication);
            }

            // Replace all instance of the stub application in ActivityThread#mAllApplications with the
            // real one
            if (realApplication != null) {
                Field mAllApplications = activityThread.getDeclaredField("mAllApplications");
                mAllApplications.setAccessible(true);
                List<Application> allApplications = (List<Application>) mAllApplications
                        .get(currentActivityThread);
                for (int i = 0; i < allApplications.size(); i++) {
                    if (allApplications.get(i) == bootstrap) {
                        allApplications.set(i, realApplication);
                    }
                }
            }

            // Figure out how loaded APKs are stored.

            // API version 8 has PackageInfo, 10 has LoadedApk. 9, I don't know.
            Class<?> loadedApkClass;
            try {
                loadedApkClass = Class.forName("android.app.LoadedApk");
            } catch (ClassNotFoundException e) {
                loadedApkClass = Class.forName("android.app.ActivityThread$PackageInfo");
            }
            Field mApplication = loadedApkClass.getDeclaredField("mApplication");
            mApplication.setAccessible(true);
//            Field mResDir = loadedApkClass.getDeclaredField("mResDir");
//            mResDir.setAccessible(true);

            // 10 doesn't have this field, 14 does. Fortunately, there are not many Honeycomb devices
            // floating around.
            Field mLoadedApk = null;
            try {
                mLoadedApk = Application.class.getDeclaredField("mLoadedApk");
            } catch (NoSuchFieldException e) {
                // According to testing, it's okay to ignore this.
            }

            // Enumerate all LoadedApk (or PackageInfo) fields in ActivityThread#mPackages and
            // ActivityThread#mResourcePackages and do two things:
            //   - Replace the Application instance in its mApplication field with the real one
            //   - Replace mResDir to point to the external resource file instead of the .apk. This is
            //     used as the asset path for new Resources objects.
            //   - Set Application#mLoadedApk to the found LoadedApk instance
            for (String fieldName : new String[]{"mPackages", "mResourcePackages"}) {
                Field field = activityThread.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(currentActivityThread);

                for (Map.Entry<String, WeakReference<?>> entry :
                        ((Map<String, WeakReference<?>>) value).entrySet()) {
                    Object loadedApk = entry.getValue().get();
                    if (loadedApk == null) {
                        continue;
                    }

                    if (mApplication.get(loadedApk) == bootstrap) {
                        if (realApplication != null) {
                            mApplication.set(loadedApk, realApplication);
                        }

                        if (realApplication != null && mLoadedApk != null) {
                            mLoadedApk.set(realApplication, loadedApk);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }


}
