package com.fotric.sdkdemo.view;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.github.gzuliyujiang.wheelpicker.OptionPicker;

public class MyOptionPicker extends OptionPicker {
    public MyOptionPicker(@NonNull Activity activity) {
        super(activity);
    }

    public MyOptionPicker(@NonNull Activity activity, int themeResId) {
        super(activity, themeResId);
    }

    @Override
    protected boolean enableMaskView() {
        return false;
    }
}
