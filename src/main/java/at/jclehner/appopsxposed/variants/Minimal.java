package at.jclehner.appopsxposed.variants;


import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import at.jclehner.appopsxposed.ApkVariant;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Minimal extends ApkVariant {
    public Minimal(SharedPreferences prefs) {
        super(prefs);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        addAppOpsToAppInfo(lpparam);
    }

    @Override
    protected boolean onMatch(ApplicationInfo appInfo, ClassChecker classChecker) {
        return false;
    }
}
