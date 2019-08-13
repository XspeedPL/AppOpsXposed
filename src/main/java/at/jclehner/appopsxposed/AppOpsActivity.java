/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013-2015 Joseph C. Lehner
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

package at.jclehner.appopsxposed;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsSummary;

import at.jclehner.appopsxposed.re.R;
import at.jclehner.appopsxposed.util.Util;
import xeed.library.common.SettingsManager;
import xeed.library.common.Utils;
import xeed.library.ui.BaseSettings;

public class AppOpsActivity extends AppCompatActivity {
    private int mThemeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsManager.getInstance(this);

        Context ctx = ContextCompat.createDeviceProtectedStorageContext(this);
        if (ctx == null) ctx = this;

        BaseSettings.reloadThemes(ctx.getSharedPreferences(Utils.PREFS_NAME, MODE_PRIVATE));
        setTheme(mThemeId = BaseSettings.getActTh());

        if (!Util.hasAppOpsPermissions(this)) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setMessage(getString(R.string.permissions_not_granted,
                    getString(R.string.app_ops_settings),
                    getString(R.string.compatibility_mode_title)));
            ab.setPositiveButton(android.R.string.ok, null);
            ab.show();
        }

        if (savedInstanceState == null) {
            Intent in = getIntent();
            String pkg = in.getStringExtra(AppOpsDetails.ARG_PACKAGE_NAME);
            Fragment frag;
            if (pkg != null) {
                frag = new AppOpsDetails();
                frag.setArguments(in.getExtras());
            } else {
                frag = new AppOpsSummary();
            }
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, frag).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mThemeId != BaseSettings.getActTh()) recreate();
    }

    @Override
    public void onBackPressed() {
        FragmentManager mgr = getSupportFragmentManager();
        if (mgr.getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            mgr.popBackStack();
        }
    }
}
