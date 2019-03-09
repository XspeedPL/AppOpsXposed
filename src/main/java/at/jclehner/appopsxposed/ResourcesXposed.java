package at.jclehner.appopsxposed;

import android.content.res.XModuleResources;

import at.jclehner.appopsxposed.util.Res;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public class ResourcesXposed implements IXposedHookInitPackageResources, IXposedHookZygoteInit {
    @Override
    public void initZygote(StartupParam param) {
        Res.modRes = XModuleResources.createInstance(param.modulePath, null);
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        if (ApkVariant.isSettingsPackage(resparam.packageName))
            Res.initializeIcons(resparam);
    }
}
