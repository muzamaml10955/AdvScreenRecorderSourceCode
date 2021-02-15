/*
 * Copyright (c) 2016-2017. Vijai Chandra Prasad R.
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

package com.coolapps.screenrecorder.preferences;

import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.coolapps.screenrecorder.Const;
import com.coolapps.screenrecorder.R;

/**
 * Created by vijai on 04-04-2017.
 */

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String androidns="http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mValueText;
    private ImageView mFloatingTogglePreview;

    private String mSuffix;
    private int mDefault, mMax, mValue = 0;

    private int defaultColor;

    private ViewGroup.LayoutParams mIV_pararams;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context,attrs);

        setPersistent(true);

        setDialogLayoutResource(R.layout.layout_floating_control_preview);

        mSuffix = attrs.getAttributeValue(androidns,"text");
        mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 100);
        mMax = attrs.getAttributeIntValue(androidns,"max", 200);
    }

    private void init(){
        mSeekBar.setOnSeekBarChangeListener(this);

        mIV_pararams = mFloatingTogglePreview.getLayoutParams();

        mFloatingTogglePreview.setLayoutParams(generateLayoutParams(mValue));

        mValueText.setText(mSuffix == null ? String.valueOf(mValue) : String.valueOf(mValue).concat(mSuffix));

        Log.d(Const.TAG, "Max: " + mMax + "     ,Progress: " + mValue);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mValue = getPersistedInt(mDefault);
        Log.d(Const.TAG, "size is: " + mValue);
        mSeekBar = v.findViewById(R.id.seekBar);
        mValueText = v.findViewById(R.id.tv_size);
        defaultColor = mValueText.getTextColors().getDefaultColor();
        mFloatingTogglePreview = v.findViewById(R.id.iv_floatingControl);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);

        init();
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 100;
        else
            mValue = (Integer)defaultValue;
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
    {
        if (value < 70)
            mValueText.setTextColor(Color.RED);
        else
            mValueText.setTextColor(defaultColor);

        if (value < 25){
            mSeekBar.setProgress(25);
            return;
        }

        String t = String.valueOf(value);
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));

        mFloatingTogglePreview.setLayoutParams(generateLayoutParams(value));

        if (shouldPersist())
            persistInt(value);
        callChangeListener(value);
    }
    public void onStartTrackingTouch(SeekBar seek) {}
    public void onStopTrackingTouch(SeekBar seek) {}

    private ViewGroup.LayoutParams generateLayoutParams(int value){
        int px = dpToPx(value);
        mIV_pararams.height = px;
        mIV_pararams.width = px;
        return mIV_pararams;
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
