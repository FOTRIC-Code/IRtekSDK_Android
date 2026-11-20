package com.fotric.sdkdemo.bean;

import com.fotric.sdkdemo.R;

public enum FuncType {

    Image(R.drawable.ic_func_image, "热像图"),
    Gain(R.drawable.ic_func_gain, "温宽信息"),
    ROI(R.drawable.ic_func_roi, "区域信息"),
    Params(R.drawable.ic_func_params, "报警参数"),
    palette(R.drawable.ic_func_palette, "调色板"),
    //    Note(R.drawable.ic_func_note, "备注"),
    CommonInfo(R.drawable.ic_setting_param, "基本信息");

    public int resId;
    public String title;

    FuncType(int resId, String title) {
        this.title = title;
        this.resId = resId;
    }

}
