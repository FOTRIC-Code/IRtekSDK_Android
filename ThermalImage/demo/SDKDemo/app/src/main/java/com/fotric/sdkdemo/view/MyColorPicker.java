package com.fotric.sdkdemo.view;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.github.gzuliyujiang.colorpicker.ColorPicker;

public class MyColorPicker extends ColorPicker {
    public MyColorPicker(@NonNull Activity activity) {
        super(activity);
    }

    public MyColorPicker(@NonNull Activity activity, int themeResId) {
        super(activity, themeResId);
    }

    @Override
    protected boolean enableMaskView() {
        return false;
    }
}
