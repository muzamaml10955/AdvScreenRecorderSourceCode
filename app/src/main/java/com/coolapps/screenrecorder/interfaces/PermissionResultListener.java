package com.coolapps.screenrecorder.interfaces;

public interface PermissionResultListener {
    void onPermissionResult(int requestCode,
                            String permissions[], int[] grantResults);
}
