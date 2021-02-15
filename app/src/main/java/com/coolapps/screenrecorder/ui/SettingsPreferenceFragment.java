/*
 * Copyright (c) 2016-2018. Vijai Chandra Prasad R.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package com.coolapps.screenrecorder.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;


import com.coolapps.screenrecorder.AdsUtility;
import com.google.android.material.snackbar.Snackbar;
import com.coolapps.screenrecorder.Const;
import com.coolapps.screenrecorder.R;
import com.coolapps.screenrecorder.folderpicker.FolderChooser;
import com.coolapps.screenrecorder.folderpicker.OnDirectorySelectedListerner;
import com.coolapps.screenrecorder.interfaces.PermissionResultListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import androidx.annotation.NonNull;

public class SettingsPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
        , PermissionResultListener, OnDirectorySelectedListerner {


    SharedPreferences prefs;

    private ListPreference res;

    private ListPreference recaudio;

    private CheckBoxPreference floatingControl;

//    private CheckBoxPreference crashReporting;

    private CheckBoxPreference usageStats;

    private FolderChooser dirChooser;

    private CheckBoxPreference cameraOverlay;

//    private CheckBoxPreference systemUIDemo;

    private MainActivity activity;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        AdsUtility.InterstitialAdmob(getActivity());

        setPermissionListener();

        String defaultSaveLoc = (new File(Environment
                .getExternalStorageDirectory() + File.separator + Const.APPDIR)).getPath();

        prefs = getPreferenceScreen().getSharedPreferences();
        res = (ListPreference) findPreference(getString(R.string.res_key));
        ListPreference fps = (ListPreference) findPreference(getString(R.string.fps_key));
        ListPreference bitrate = (ListPreference) findPreference(getString(R.string.bitrate_key));
        recaudio = (ListPreference) findPreference(getString(R.string.audiorec_key));
        ListPreference filenameFormat = (ListPreference) findPreference(getString(R.string.filename_key));
        EditTextPreference filenamePrefix = (EditTextPreference) findPreference(getString(R.string.fileprefix_key));
        dirChooser = (FolderChooser) findPreference(getString(R.string.savelocation_key));
        floatingControl = (CheckBoxPreference) findPreference(getString(R.string.preference_floating_control_key));
        usageStats = (CheckBoxPreference) findPreference(getString(R.string.preference_anonymous_statistics_key));
        //Set previously chosen directory as initial directory
        dirChooser.setCurrentDir(getValue(getString(R.string.savelocation_key), defaultSaveLoc));
        cameraOverlay = (CheckBoxPreference) findPreference(getString(R.string.preference_camera_overlay_key));


//        Preference privacypolicy = findPreference(getString(R.string.preference_privacypolicy_key));
//        privacypolicy.setOnPreferenceClickListener(preference -> {
//            startActivity(new Intent(getActivity(), PrivacyPolicy.class));
//            return false;
//        });

        ListPreference orientation = (ListPreference) findPreference(getString(R.string.orientation_key));
        orientation.setSummary(orientation.getEntry());

        ListPreference theme = (ListPreference) findPreference(getString(R.string.preference_theme_key));
        theme.setSummary(theme.getEntry());

        checkNativeRes(res);
        updateResolution(res);

        fps.setSummary(getValue(getString(R.string.fps_key), "30"));
        float bps = bitsToMb(Integer.parseInt(getValue(getString(R.string.bitrate_key), "7130317")));
        bitrate.setSummary(bps + " Mbps");
        dirChooser.setSummary(getValue(getString(R.string.savelocation_key), defaultSaveLoc));
        filenameFormat.setSummary(getFileSaveFormat());
        filenamePrefix.setSummary(getValue(getString(R.string.fileprefix_key), "recording"));

        checkAudioRecPermission();

        if (floatingControl.isChecked())
            requestSystemWindowsPermission(Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE);

        if (cameraOverlay.isChecked()) {
            requestCameraPermission();
            requestSystemWindowsPermission(Const.CAMERA_SYSTEM_WINDOWS_CODE);
        }
        dirChooser.setOnDirectoryClickedListerner(this);
    }

    private void checkNativeRes(ListPreference res) {

        ArrayList<String> resEntries = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.resolutionsArray)));
        ArrayList<String> resEntryValues = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.resolutionValues)));

        String nativeRes = getNativeRes();

        boolean hasValuesChanged = false;

        for (String resolution : resEntryValues) {
            if (Integer.parseInt(resolution) > Integer.parseInt(nativeRes)) {
                resEntries.remove(resolution + "P");
                resEntryValues.remove(resolution);
                hasValuesChanged = true;
                Log.d(Const.TAG, "Removed " + resolution + " from entries");
            }
        }

        if (!resEntryValues.contains(nativeRes)) {
            Log.d(Const.TAG, "Add native res! " + nativeRes);
            resEntries.add(nativeRes + "P");
            resEntryValues.add(nativeRes);
            hasValuesChanged = true;
        }

        if (hasValuesChanged) {
            res.setEntries(resEntries.toArray(new CharSequence[resEntries.size()]));
            res.setEntryValues(resEntryValues.toArray(new CharSequence[resEntryValues.size()]));
        }
    }

    private void checkAudioRecPermission() {
        String value = recaudio.getValue();
        switch (value) {
            case "1":
                requestAudioPermission(Const.AUDIO_REQUEST_CODE);
                break;
            case "2":
                requestAudioPermission(Const.INTERNAL_AUDIO_REQUEST_CODE);
                break;
        }
        recaudio.setSummary(recaudio.getEntry());
    }

    private void updateResolution(ListPreference pref) {
        String resolution = getValue(getString(R.string.res_key), getNativeRes());
        if (resolution.toLowerCase().contains("x")) {
            resolution = getNativeRes();
            pref.setValue(resolution);
        }
        pref.setSummary(resolution + "P");
    }

    private String getNativeRes() {
        DisplayMetrics metrics = getRealDisplayMetrics();
        return String.valueOf(getScreenWidth(metrics));
    }

    private ArrayList<String> buildEntries(int resID) {
        DisplayMetrics metrics = getRealDisplayMetrics();
        int deviceWidth = getScreenWidth(metrics);
        ArrayList<String> entries = new ArrayList<>(Arrays.asList(getResources().getStringArray(resID)));
        Iterator<String> entriesIterator = entries.iterator();
        while (entriesIterator.hasNext()) {
            String width = entriesIterator.next();
            if (deviceWidth < Integer.parseInt(width)) {
                entriesIterator.remove();
            }
        }
        if (!entries.contains("" + deviceWidth))
            entries.add("" + deviceWidth);
        return entries;
    }

    private DisplayMetrics getRealDisplayMetrics(){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager window = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getRealMetrics(metrics);
        return metrics;
    }

    private int getScreenWidth(DisplayMetrics metrics) {
        return metrics.widthPixels;
    }

    private int getScreenHeight(DisplayMetrics metrics) {
        return metrics.heightPixels;
    }

    @Deprecated
    private Const.ASPECT_RATIO getAspectRatio() {
        float screen_width = getScreenWidth(getRealDisplayMetrics());
        float screen_height = getScreenHeight(getRealDisplayMetrics());
        float aspectRatio;
        if (screen_width > screen_height) {
            aspectRatio = screen_width / screen_height;
        } else {
            aspectRatio = screen_height / screen_width;
        }
        return Const.ASPECT_RATIO.valueOf(aspectRatio);
    }

    private void setPermissionListener() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            activity = (MainActivity) getActivity();
            activity.setPermissionResultListener(this);
        }
    }

    private String getValue(String key, String defVal) {
        return prefs.getString(key, defVal);
    }

    private float bitsToMb(float bps) {
        return bps / (1024 * 1024);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Preference pref = findPreference(s);
        if (pref == null) return;
        switch (pref.getTitleRes()) {
            case R.string.preference_resolution_title:
                updateResolution((ListPreference) pref);
                AdsUtility.showIntestitialAds();
                break;
            case R.string.preference_fps_title:
                String fps = String.valueOf(getValue(getString(R.string.fps_key), "30"));
                pref.setSummary(fps);
                AdsUtility.showIntestitialAds();
                break;
            case R.string.preference_bit_title:
                float bps = bitsToMb(Integer.parseInt(getValue(getString(R.string.bitrate_key), "7130317")));
                pref.setSummary(bps + " Mbps");
                if (bps > 12)
                    Toast.makeText(getActivity(), R.string.toast_message_bitrate_high_warning, Toast.LENGTH_SHORT).show();
                break;
            case R.string.preference_filename_format_title:
                pref.setSummary(getFileSaveFormat());
                break;
            case R.string.preference_audio_record_title:
                switch (recaudio.getValue()) {
                    case "1":
                        requestAudioPermission(Const.AUDIO_REQUEST_CODE);
                        break;
                    case "2":
                        if (!prefs.getBoolean(Const.PREFS_INTERNAL_AUDIO_DIALOG_KEY, false))
                            showInternalAudioWarning(false);
                        else
                            requestAudioPermission(Const.INTERNAL_AUDIO_REQUEST_CODE);
                        break;
                    case "3":
                        if (!Const.IS_MAGISK_MODE) {
                            Toast.makeText(getActivity(), getString(R.string.toast_magisk_module_required_message), Toast.LENGTH_SHORT).show();
                            recaudio.setValue("0");
                            break;
                        }

                        if (!prefs.getBoolean(Const.PREFS_INTERNAL_AUDIO_DIALOG_KEY, false))
                            showInternalAudioWarning(true);
                        else
                            requestAudioPermission(Const.INTERNAL_R_SUBMIX_AUDIO_REQUEST_CODE);
                        break;
                    default:
                        recaudio.setValue("0");
                        break;
                }
                pref.setSummary(((ListPreference) pref).getEntry());
                break;
            case R.string.preference_filename_prefix_title:
                EditTextPreference etp = (EditTextPreference) pref;
                etp.setSummary(etp.getText());
                ListPreference filename = (ListPreference) findPreference(getString(R.string.filename_key));
                filename.setSummary(getFileSaveFormat());
                break;
            case R.string.preference_floating_control_title:
                requestSystemWindowsPermission(Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE);
                break;
//            case R.string.preference_show_touch_title:
//                CheckBoxPreference showTouchCB = (CheckBoxPreference)pref;
//                if (showTouchCB.isChecked() && !hasPluginInstalled()){
//                    showTouchCB.setChecked(false);
//                    showDownloadAlert();
//                }
//                break;
//            case R.string.preference_crash_reporting_title:
//                CheckBoxPreference crashReporting = (CheckBoxPreference)pref;
//                CheckBoxPreference anonymousStats = (CheckBoxPreference) findPreference(getString(R.string.preference_anonymous_statistics_key));
//                if(!crashReporting.isChecked())
//                    anonymousStats.setChecked(false);
//            case R.string.preference_anonymous_statistics_title:
//                Toast.makeText(getActivity(), R.string.toast_message_countly_activity_restart, Toast.LENGTH_SHORT).show();
//                activity.recreate();
//                break;
            case R.string.preference_theme_title:
                activity.recreate();
                break;
            case R.string.preference_orientation_title:
                AdsUtility.showIntestitialAds();
                pref.setSummary(((ListPreference) pref).getEntry());
                break;
            case R.string.preference_camera_overlay_title:
                requestCameraPermission();
                break;
//            case R.string.preference_sysui_demo_mode_title:
//                if (Shell.rootAccess())
//                    checkDUMPPermission();
//                else {
//                    systemUIDemo.setChecked(false);
//                    Toast.makeText(getActivity(), getString(R.string.toast_msg_root_failed), Toast.LENGTH_SHORT).show();
//                }
//                break;
        }
    }

    private void showInternalAudioWarning(boolean isR_submix) {
        int message;
        final int requestCode;
        if (isR_submix) {
            message = R.string.alert_dialog_r_submix_audio_warning_message;
            requestCode = Const.INTERNAL_R_SUBMIX_AUDIO_REQUEST_CODE;
        } else {
            message = R.string.alert_dialog_internal_audio_warning_message;
            requestCode = Const.INTERNAL_AUDIO_REQUEST_CODE;
        }
//        new AlertDialog.Builder(activity)
//                .setTitle(R.string.alert_dialog_internal_audio_warning_title)
//                .setMessage(message)
//                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        requestAudioPermission(requestCode);
//
//                    }
//                })
//                .setNeutralButton(R.string.alert_dialog_internal_audio_warning_faq_text, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        startActivity(new Intent(getActivity(), FAQActivity.class));
//                    }
//                })
//                .setNegativeButton(R.string.alert_dialog_internal_audio_warning_negative_btn_text, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        prefs.edit().putBoolean(Const.PREFS_INTERNAL_AUDIO_DIALOG_KEY, true)
//                                .apply();
//                        requestAudioPermission(Const.INTERNAL_AUDIO_REQUEST_CODE);
//                    }
//                })
//                .setCancelable(false)
//                .create()
//                .show();
    }

    public String getFileSaveFormat() {
        String filename = prefs.getString(getString(R.string.filename_key), "yyyyMMdd_HHmmss").replace("hh", "HH");
        String prefix = prefs.getString(getString(R.string.fileprefix_key), "recording");
        return prefix + "_" + filename;
    }

    public void requestAudioPermission(int requestCode) {
        if (activity != null) {
            activity.requestPermissionAudio(requestCode);
        }
    }

    public void requestCameraPermission() {
        if (activity != null)
            activity.requestPermissionCamera();
    }

    private void requestSystemWindowsPermission(int code) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestSystemWindowsPermission(code);
        } else {
            Log.d(Const.TAG, "API is < 23");
        }
    }

    private void showSnackbar() {
        Snackbar.make(getActivity().findViewById(R.id.fab), R.string.snackbar_storage_permission_message,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_storage_permission_action_enable,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (activity != null){
                            activity.requestPermissionStorage();
                        }
                    }
                }).show();
    }

    private void showPermissionDeniedDialog(){
        new AlertDialog.Builder(activity)
                .setTitle(R.string.alert_permission_denied_title)
                .setMessage(R.string.alert_permission_denied_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (activity != null){
                            activity.requestPermissionStorage();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showSnackbar();
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(false)
                .create().show();
    }

    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                    Log.d(Const.TAG, "Storage permission denied. Requesting again");
                    dirChooser.setEnabled(false);
                    showPermissionDeniedDialog();
                } else if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    dirChooser.setEnabled(true);
                }
                return;
            case Const.AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Record audio permission granted.");
                    recaudio.setValue("1");
                } else {
                    Log.d(Const.TAG, "Record audio permission denied");
                    recaudio.setValue("0");
                }
                recaudio.setSummary(recaudio.getEntry());
                return;
            case Const.INTERNAL_AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Record audio permission granted.");
                    recaudio.setValue("2");
                } else {
                    Log.d(Const.TAG, "Record audio permission denied");
                    recaudio.setValue("0");
                }
                recaudio.setSummary(recaudio.getEntry());
                return;
            case Const.INTERNAL_R_SUBMIX_AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Record audio permission granted.");
                    recaudio.setValue("3");
                } else {
                    Log.d(Const.TAG, "Record audio permission denied");
                    recaudio.setValue("0");
                }
                recaudio.setSummary(recaudio.getEntry());
                return;
            case Const.FLOATING_CONTROLS_SYSTEM_WINDOWS_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "System Windows permission granted");
                    floatingControl.setChecked(true);
                } else {
                    Log.d(Const.TAG, "System Windows permission denied");
                    floatingControl.setChecked(false);
                }
                return;
            case Const.CAMERA_SYSTEM_WINDOWS_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "System Windows permission granted");
                    cameraOverlay.setChecked(true);
                } else {
                    Log.d(Const.TAG, "System Windows permission denied");
                    cameraOverlay.setChecked(false);
                }
                return;
            case Const.CAMERA_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "System Windows permission granted");
                    requestSystemWindowsPermission(Const.CAMERA_SYSTEM_WINDOWS_CODE);
                } else {
                    Log.d(Const.TAG, "System Windows permission denied");
                    cameraOverlay.setChecked(false);
                }
            default:
                Log.d(Const.TAG, "Unknown permission request with request code: " + requestCode);
        }
    }

    @Override
    public void onDirectorySelected() {
        Log.d(Const.TAG, "In settings fragment");
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onDirectoryChanged();
        }
    }

}
