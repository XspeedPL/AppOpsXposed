package at.jclehner.appopsxposed.hacks;

import android.content.SharedPreferences;

import at.jclehner.appopsxposed.Hack;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DontGroupOpsHack extends Hack {
    public DontGroupOpsHack(SharedPreferences prefs) {
        super(prefs);
    }

    @Override
    protected void handleLoadAnyPackage(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager",
                    lpparam.classLoader, "opToSwitch", int.class, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(param.args[0]);
                        }
                    });
        } catch (Throwable t) {
            debug(t);
        }
    }

    @Override
    protected String onGetKeySuffix() {
        return "dont_group_ops";
    }
}
