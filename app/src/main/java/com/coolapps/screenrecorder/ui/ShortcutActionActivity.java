package com.coolapps.screenrecorder.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.coolapps.screenrecorder.Const;
import com.coolapps.screenrecorder.R;
import com.coolapps.screenrecorder.services.RecorderService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ShortcutActionActivity extends AppCompatActivity {

    private MediaProjectionManager mProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.shortcut_storage_permission_request_title), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.preference_theme_key), Const.PREFS_LIGHT_THEME);
        //Acquiring media projection service to start screen mirroring
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (getIntent().getAction() != null) {
            if (getIntent().getAction().equals(getString(R.string.app_shortcut_action))) {
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        String intentAction = getIntent().getAction();

        if (resultCode == RESULT_CANCELED && requestCode == Const.SCREEN_RECORD_REQUEST_CODE) {
            Toast.makeText(this,
                    getString(R.string.screen_recording_permission_denied), Toast.LENGTH_SHORT).show();
            if (intentAction != null && intentAction.equals(getString(R.string.app_shortcut_action)))
                this.finish();
            return;

        }

        Intent recorderService = new Intent(this, RecorderService.class);
        recorderService.setAction(Const.SCREEN_RECORDING_START);
        recorderService.putExtra(Const.RECORDER_INTENT_DATA, data);
        recorderService.putExtra(Const.RECORDER_INTENT_RESULT, resultCode);
        startService(recorderService);

        if (intentAction != null && intentAction.equals(getString(R.string.app_shortcut_action)))
            this.finish();
    }
}
