package com.fotric.sdkdemo.bean;

import android.graphics.Bitmap;

public class PalatteInfoItem {

    public String key;
    public Bitmap image;
    public byte refNo;
    public boolean selected = false;

    public PalatteInfoItem(String key,byte refNo, Bitmap image) {
        this.key = key;
        this.refNo = refNo;
        this.image = image;
    }
}
