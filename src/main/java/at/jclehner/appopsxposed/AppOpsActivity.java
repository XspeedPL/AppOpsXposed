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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsSummary;

import at.jclehner.appopsxposed.re.R;
import at.jclehner.appopsxposed.util.Util;
import xeed.library.common.SettingsManager;
import xeed.library.ui.BaseSettings;

public class AppOpsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = SettingsManager.getInstance(this).getPrefs();
        BaseSettings.reloadThemes(prefs);
        setTheme(BaseSettings.getActTh());

        if (!Util.hasAppOpsPermissions(this)) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setMessage(getString(R.string.permissions_not_granted,
                    getString(R.string.app_ops_settings),
                    getString(R.string.compatibility_mode_title)));
            ab.setPositiveButton(android.R.string.ok, null);
            ab.show();
        }

        Intent in = getIntent();
        String pkg = in.getStringExtra("package");
        if ("android.settings.APP_OPS_SETTINGS".equals(in.getAction()) || pkg != null) {
            if (pkg != null) {
                AppOpsDetails details = new AppOpsDetails();
                details.setArguments(in.getExtras());
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content, details).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new AppOpsSummary()).commit();
            }
        } else if (!in.hasExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT)) {
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new AppOpsSummary()).commit();
        }
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
