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

package at.jclehner.appopsxposed;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Button;
import android.widget.Toast;

import at.jclehner.appopsxposed.re.BuildConfig;
import at.jclehner.appopsxposed.re.R;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.Constants;
import at.jclehner.appopsxposed.util.OpsLabelHelper;
import at.jclehner.appopsxposed.util.Util;
import xeed.library.ui.BaseSettings;

public class SettingsActivity extends BaseSettings {
    private static final int REQUEST_CREATE_BACKUP = 0;
    private static final int REQUEST_RESTORE_BACKUP = 1;

    @Override
    protected void onCreatePreferences(PreferenceManager mgr) {
        hideDonations();

        addPreferencesToCategory(R.xml.settings, Category.general);
        final SharedPreferences prefs = mgr.getSharedPreferences();

        ListPreference lp = (ListPreference) mgr.findPreference("force_variant");
        onPreferenceChanged(mgr, prefs, lp.getKey());

        CharSequence[] entries = lp.getEntries();
        CharSequence[] values = new CharSequence[entries.length];
        System.arraycopy(entries, 0, values, 0, entries.length);
        values[0] = "";
        lp.setEntryValues(values);
        lp.setOnPreferenceChangeListener(this);

        Preference p /*= findPreference("failsafe_mode");
			callOnChangeListenerWithCurrentValue(p);
			p.setOnPreferenceChangeListener(this)*/;

        p = mgr.findPreference("use_hack_boot_completed");
        p.setEnabled(!AppOpsManagerWrapper.hasTrueBootCompletedOp());
        p.setSummary(getString(R.string.use_hack_boot_completed_summary,
                getString(R.string.app_ops_labels_post_notification),
                getString(R.string.app_ops_labels_vibrate)));

        p = mgr.findPreference("use_hack_wake_lock");
        p.setSummary(getString(R.string.use_hack_wake_lock_summary,
                OpsLabelHelper.getOpLabel(this, "OP_WAKE_LOCK")));

        p = mgr.findPreference("use_hack_pm_crash");
        if (p != null) {
            p.setSummary(getString(R.string.use_hack_pm_crash_summary,
                    getString(R.string.app_ops_settings)));
        }

        p = mgr.findPreference("hacks");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (prefs.getBoolean("show_hacks_warning_dialog", true)) {
                    showHacksWarningDialog(prefs, preference);
                    return true;
                } else return false;
            }
        });

        String[] keys = {"icon_appinfo", "icon_settings"};
        for (String key : keys) {
            p = mgr.findPreference(key);
            ((IconPreference) p).setIcons(Constants.ICONS);
        }

        keys = new String[]{"backup_create", "backup_restore"};
        for (String key : keys) {
            p = mgr.findPreference(key);
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    int status = ContextCompat.checkSelfPermission(SettingsActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    boolean create = "backup_create".equals(preference.getKey());

                    if (status == PackageManager.PERMISSION_GRANTED)
                        createOrRestoreBackup(create);
                    else {
                        ActivityCompat.requestPermissions(SettingsActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                create ? REQUEST_CREATE_BACKUP : REQUEST_RESTORE_BACKUP);
                    }

                    return true;
                }
            });
        }

        if (!BuildConfig.DEBUG) {
            p = mgr.findPreference("use_hack_dont_group_ops");
            if (p != null) {
                Preference ps = mgr.findPreference("hacks");
                if (ps instanceof PreferenceScreen)
                    ((PreferenceScreen) ps).removePreference(p);
            }
        }
    }

    @Override
    protected void onPreferenceChanged(PreferenceManager mgr, SharedPreferences prefs, String key) {
        Preference pref = mgr.findPreference(key);
        if ("force_variant".equals(key)) {
            String variant = prefs.getString(key, "");
            if (variant.isEmpty())
                pref.setSummary(R.string.automatic);
            else
                pref.setSummary(variant);
        } else if ("failsafe_mode".equals(key)) {
            boolean failsafe = prefs.getBoolean(key, false);

            if (failsafe && !prefs.getBoolean("show_launcher_icon", true)) {
                CheckBoxPreference p = (CheckBoxPreference) mgr.findPreference("show_launcher_icon");
                p.setChecked(true);
            }

            mgr.findPreference("show_launcher_icon").setEnabled(!failsafe);
            mgr.findPreference("force_variant").setEnabled(!failsafe);
            //mgr.findPreference("use_layout_fix").setEnabled(!failsafe);
            mgr.findPreference("hacks").setEnabled(!failsafe);
        } else if ("compatibility_mode".equals(key)) {
            boolean useCompatibilityMode = prefs.getBoolean(key, false);

            if (useCompatibilityMode && !prefs.getBoolean("show_launcher_icon", true)) {
                CheckBoxPreference p = (CheckBoxPreference) mgr.findPreference("show_launcher_icon");
                p.setChecked(true);

                Toast.makeText(this, R.string.must_reboot_device, Toast.LENGTH_LONG).show();
            }

            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, "at.jclehner.appopsxposed.LauncherActivity$HtcActivity"),
                    useCompatibilityMode ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            pm.setComponentEnabledSetting(new ComponentName(this, "at.jclehner.appopsxposed.LauncherActivity$HtcFragment"),
                    useCompatibilityMode ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CREATE_BACKUP || requestCode == REQUEST_RESTORE_BACKUP) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                createOrRestoreBackup(requestCode == REQUEST_CREATE_BACKUP);
            else
                Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void createOrRestoreBackup(boolean create) {
        boolean status = create ? Backup.create(this)
                : Backup.restore(this);

        Toast.makeText(this, status ? R.string.success
                : R.string.failed, Toast.LENGTH_SHORT).show();
    }

    private void showHacksWarningDialog(final SharedPreferences prefs, Preference pref) {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setCancelable(false);
        ab.setIcon(android.R.drawable.ic_dialog_alert);
        ab.setTitle(R.string.hacks_dialog_title);
        ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean("show_hacks_warning_dialog", false).apply();
            }
        });
        ab.setNegativeButton(android.R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                getSupportFragmentManager().popBackStack();
            }
        });

        final AlertDialog dialog = ab.create();
        dialog.setMessage(Util.fromHtml(getString(R.string.hacks_dialog_message)));
        dialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                final Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(false);
                final CharSequence origText = b.getText();

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 9; i > 0; --i) {
                            final String tempText =
                                    origText + " (" + i + ")";

                            b.post(new Runnable() {

                                @Override
                                public void run() {
                                    b.setText(tempText);
                                }
                            });

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {
                            }
                        }

                        b.post(new Runnable() {

                            @Override
                            public void run() {
                                b.setText(origText);
                                b.setEnabled(true);
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }
}
