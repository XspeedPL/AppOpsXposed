/**
 * Copyright (C) 2013 The Android Open Source Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import at.jclehner.appopsxposed.re.R;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.OpEntryWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.PackageOpsWrapper;

public class AppOpsDetails extends Fragment {
    static final String TAG = "AppOpsDetails";

    public static final String ARG_PACKAGE_NAME = "at.jclehner.appopsxposed.re.Package";

    private AppOpsState mState;
    private PackageManager mPm;
    private AppOpsManagerWrapper mAppOps;
    private PackageInfo mPackageInfo;
    private LayoutInflater mInflater;
    private View mRootView;
    private TextView mAppVersion;
    private LinearLayout mOperationsSection;

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        View appSnippet = mRootView.findViewById(R.id.app_snippet);
        appSnippet.setPaddingRelative(0, appSnippet.getPaddingTop(), 0, appSnippet.getPaddingBottom());

        ImageView icon = appSnippet.findViewById(R.id.app_icon);
        icon.setImageDrawable(mPm.getApplicationIcon(pkgInfo.applicationInfo));
        // Set application name.
        TextView label = appSnippet.findViewById(R.id.app_name);
        label.setText(mPm.getApplicationLabel(pkgInfo.applicationInfo));
        // Version number of application
        mAppVersion = appSnippet.findViewById(R.id.app_size);

        StringBuilder sb = new StringBuilder(pkgInfo.packageName);

        if (pkgInfo.versionName != null) {
            sb.append("\n");
            sb.append(getString(R.string.version_text, pkgInfo.versionName));
        }

        mAppVersion.setText(sb);
    }

    private String retrieveAppEntry() {
        String packageName = requireArguments().getString(ARG_PACKAGE_NAME);
        try {
            mPackageInfo = mPm.getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                            PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + packageName, e);
            mPackageInfo = null;
        }

        if (requireActivity().getPackageName().equals(packageName)) {
            Toast.makeText(getActivity(), "\uD83D\uDE22", Toast.LENGTH_SHORT).show();
        }

        return packageName;
    }

    private boolean refreshUi() {
        if (mPackageInfo == null) {
            return false;
        }

        setAppLabelAndIcon(mPackageInfo);

        Resources res = requireActivity().getResources();

        mOperationsSection.removeAllViews();
        boolean hasBootupSwitch = false;
        String lastPermGroup = "";
        for (AppOpsState.OpsTemplate tpl : AppOpsState.ALL_TEMPLATES) {
            List<AppOpsState.AppOpEntry> entries = mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName);
            for (final AppOpsState.AppOpEntry entry : entries) {
                OpEntryWrapper firstOp = entry.getOpEntry(0);
                View view = mInflater.inflate(R.layout.app_ops_details_item,
                        mOperationsSection, false);
                String perm = AppOpsManagerWrapper.opToPermission(firstOp.getOp());
                if (perm != null) {
                    if (Manifest.permission.RECEIVE_BOOT_COMPLETED.equals(perm)) {
                        if (!hasBootupSwitch) {
                            hasBootupSwitch = true;
                        } else {
                            Log.i(TAG, "Skipping second bootup switch");
                            continue;
                        }
                    }
                    try {
                        PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                                ((ImageView) view.findViewById(R.id.op_icon)).setImageDrawable(
                                        pgi.loadIcon(mPm));
                            }
                        }
                    } catch (NameNotFoundException ignored) {
                    }
                }
                ((TextView) view.findViewById(R.id.op_name)).setText(
                        entry.getSwitchText(getActivity(), mState));
                ((TextView) view.findViewById(R.id.op_time)).setText(
                        entry.getTimeText(res, true));

                Switch sw = view.findViewById(R.id.switchWidget);
                final int switchOp = AppOpsManagerWrapper.opToSwitch(firstOp.getOp());
                sw.setChecked(modeToChecked(switchOp, entry.getPackageOps()));
                sw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
                                entry.getPackageOps().getPackageName(), isChecked
                                        ? AppOpsManagerWrapper.MODE_ALLOWED : AppOpsManagerWrapper.MODE_IGNORED);
                    }
                });
                mOperationsSection.addView(view);
            }
        }

        return true;
    }

    private boolean modeToChecked(int switchOp, PackageOpsWrapper ops) {
        return modeToChecked(mAppOps.checkOpNoThrow(switchOp, ops.getUid(), ops.getPackageName()));
    }

    static boolean modeToChecked(int mode) {
        return mode == AppOpsManagerWrapper.MODE_ALLOWED
                || mode == AppOpsManagerWrapper.MODE_DEFAULT
                || mode == AppOpsManagerWrapper.MODE_ASK
                || mode == AppOpsManagerWrapper.MODE_HINT;
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        requireArguments().putBoolean("chg", appChanged);
        if (finish) {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .remove(this).commit();
        }

        //Intent intent = new Intent();
        //intent.putExtra("chg", appChanged);
        //getActivity().finishPreferencePanel(this, Activity.RESULT_OK, intent);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mState = new AppOpsState(getActivity());
        mPm = requireActivity().getPackageManager();
        mInflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAppOps = AppOpsManagerWrapper.from(getActivity());

        retrieveAppEntry();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_ops_details, container, false);
        //Utils.prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        mOperationsSection = view.findViewById(R.id.operations_section);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }
}
