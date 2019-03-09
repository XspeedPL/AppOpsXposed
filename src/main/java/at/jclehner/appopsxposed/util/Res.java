package at.jclehner.appopsxposed.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import at.jclehner.appopsxposed.AppOpsXposed;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public class Res {
    private static final int[] mIcons = new int[Constants.ICONS.length];
    private static boolean mIconsLoaded;

    public static Resources settingsRes;
    public static Resources modRes;

    public static void initializeIcons(XC_InitPackageResources.InitPackageResourcesParam resParam) {
        for (int i = 0; i != mIcons.length; ++i) {
            mIcons[i] = resParam.res.addResource(Res.modRes, Constants.ICONS[i]);
        }
        mIconsLoaded = true;
    }

    public static int getSettingsIdentifier(String name) {
        return settingsRes.getIdentifier(name, null, AppOpsXposed.SETTINGS_PACKAGE);
    }

    private static void initializeModRes(PackageManager manager) {
        if (modRes != null) return;
        try {
            modRes = manager.getResourcesForApplication(AppOpsXposed.MODULE_PACKAGE);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initializeAppRes(PackageManager manager) {
        if (settingsRes != null) return;
        try {
            settingsRes = manager.getResourcesForApplication(AppOpsXposed.MODULE_PACKAGE);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSettingsString(Context context, @StringRes int resId) {
        initializeAppRes(context.getPackageManager());
        return settingsRes.getString(resId);
    }

    public static String getModString(Context context, @StringRes int resId) {
        initializeModRes(context.getPackageManager());
        return modRes.getString(resId);
    }

    public static Drawable getModDrawable(Context context, @DrawableRes int resId) {
        initializeModRes(context.getPackageManager());
        return modRes.getDrawable(resId);
    }

    public static int getIcon(int index) {
        return mIconsLoaded ? mIcons[index] : android.R.drawable.ic_menu_preferences;
    }
}
