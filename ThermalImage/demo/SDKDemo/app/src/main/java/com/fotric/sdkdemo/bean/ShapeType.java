package com.fotric.sdkdemo.bean;

import com.fotric.sdkdemo.R;

public enum ShapeType {

    Point(R.drawable.ic_shape_point, "点"),
    Line(R.drawable.ic_shape_line, "线"),
    Rect(R.drawable.ic_shape_rect, "矩形"),
    Circle(R.drawable.ic_shape_circle, "圆");

    public int resId;
    public String title;

    ShapeType(int resId, String title) {
        this.title = title;
        this.resId = resId;
    }

}
