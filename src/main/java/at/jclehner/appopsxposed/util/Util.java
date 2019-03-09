/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013 Joseph C. Lehner
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jclehner.appopsxposed.util;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.android.settings.applications.AppOpsDetails;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import at.jclehner.appopsxposed.AppOpsActivity;
import at.jclehner.appopsxposed.re.BuildConfig;
import dalvik.system.DexFile;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import xeed.library.ui.BaseSettings;

public final class Util {
    public static boolean sIsBootCompletedHackWorking = false;

    public interface Logger {
        void log(String s);

        void log(Throwable t);
    }

    public static Logger logger = new Logger() {

        @Override
        public void log(Throwable t) {
            Log.w("AOX", t);
        }

        @Override
        public void log(String s) {
            Log.i("AOX", s);
        }
    };

    private static final int logLevel = BuildConfig.DEBUG ? 100 : 1;

    public static void log(Throwable t) {
        if (logLevel >= 1)
            logger.log(t);
    }

    public static void log(String s) {
        if (logLevel >= 1)
            logger.log(s);
    }

    public static void debug(Throwable t) {
        if (logLevel >= 2)
            logger.log(t);
    }

    public static void debug(String s) {
        if (logLevel >= 2)
            logger.log(s);
    }

    public static boolean isBootCompletedHackWorking() {
        return sIsBootCompletedHackWorking;
    }

    public static boolean isXposedModuleOrSystemApp(Context context) {
        return BaseSettings.getActiveVer() != -1 || isSystemApp(context);
    }

    public static boolean containsManufacturer(String str) {
        return Build.MANUFACTURER.toLowerCase(Locale.US).contains(str.toLowerCase());
    }

    public static String getSystemProperty(String key, String defValue) {
        try {
            Method m = Class.forName("android.os.SystemProperties").
                    getMethod("get", String.class, String.class);

            return (String) m.invoke(null, key, defValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defValue;
        }
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    public static Intent createAppOpsIntent(String packageName) {
        Intent intent = new Intent();
        intent.setClassName("at.jclehner.appopsxposed.re",
                AppOpsActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        if (packageName != null) {
            intent.putExtra(AppOpsDetails.ARG_PACKAGE_NAME, packageName);
        }

        return intent;
    }

    public static boolean hasAppOpsPermissions(Context context) {
        for (String perm : Constants.APP_OPS_PERMISSIONS) {
            if (context.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    public static Set<String> getClassList(String apkFile, String packageName, boolean getSubPackages) {
        Enumeration<String> entries;
        DexFile df = null;
        try {
            df = new DexFile(apkFile);
            entries = df.entries();
        } catch (IOException e) {
            debug(e);
            return null;
        } finally {
            Util.closeQuietly(df);
        }

        Set<String> classes = new HashSet<>();

        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();

            if (packageName != null) {
                if (!entry.startsWith(packageName))
                    continue;

                if (!getSubPackages && entry.substring(packageName.length() + 1).contains("."))
                    continue;
            }

            classes.add(entry);
        }

        return classes;
    }

    public static Set<String> getClassList(LoadPackageParam lpparam, String packageName, boolean getSubPackages) {
        if (lpparam.appInfo == null)
            return null;

        return getClassList(lpparam.appInfo.sourceDir, packageName, getSubPackages);
    }

    public static String capitalizeFirst(CharSequence text) {
        if (text == null)
            return null;

        if (text.length() == 0 || !Character.isLowerCase(text.charAt(0)))
            return text.toString();

        return Character.toUpperCase(text.charAt(0)) + text.subSequence(1, text.length()).toString();
    }

    private static boolean isSystemApp(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            return 0 != (appInfo.flags & ApplicationInfo.FLAG_SYSTEM);
        } catch (NameNotFoundException e) {
            // shouldn't happen
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int getOpValue(String opName) {
        try {
            return AppOpsManager.class.getField(opName).getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            return -1;
        }
    }

    private static void closeQuietly(DexFile df) {
        try {
            if (df != null)
                df.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static void closeQuietly(Closeable c) {
        try {
            if (c != null)
                c.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static class StringList {
        private final List<CharSequence> mList = new ArrayList<>();

        public void add(String string) {
            mList.add(string);
        }

        public boolean isEmpty() {
            return mList.isEmpty();
        }

        @NonNull
        @Override
        public String toString() {
            if (mList.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();

            for (CharSequence str : mList) {
                sb.append(str).append(", ");
            }

            return sb.substring(0, sb.length() - 2);
        }
    }

    private Util() {
    }
}
