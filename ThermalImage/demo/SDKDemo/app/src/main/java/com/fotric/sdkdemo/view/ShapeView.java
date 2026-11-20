package com.fotric.sdkdemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.fotric.sdkdemo.bean.ShapeType;
import com.fotric.sdkdemo.utils.DensityUtil;

public class ShapeView extends View {

    public ShapeType mShapeType;
    private Paint mPaint;
    private int mLineWidth = 3;

    public ShapeView(Context context, ShapeType shapeType) {
        super(context);
        mShapeType = shapeType;
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        mPaint.setStyle(Paint.Style.STROKE);
        switch (mShapeType) {
            case Line:
                canvas.drawLine(0, 0, width, height, mPaint);
                break;
            case Point:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(width / 2, height / 2, height, mPaint);
                break;
            case Rect:
                canvas.drawRect(new RectF(mLineWidth / 2, mLineWidth / 2, width - mLineWidth / 2, height - mLineWidth / 2), mPaint);
                break;
            case Circle:
                canvas.drawCircle(width / 2, height / 2, height / 2, mPaint);
                break;
        }
    }

    private void init() {
        mLineWidth = DensityUtil.dp2px(1.5f);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mLineWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GREEN);
    }
}
