package com.fotric.sdkdemo.view;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class ParentIRView extends RelativeLayout {

    private IClickPositionCallback mIClickPositionCallback;

    public ParentIRView(Context context) {
        super(context);
    }

    public ParentIRView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ParentIRView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        float x = motionEvent.getX();
                        float y = motionEvent.getY();
                        if (mIClickPositionCallback != null) {
                            mIClickPositionCallback.click(x / getWidth(), y / getHeight());
                        }
                        break;
                }

                return true;
            }
        });
    }

    public void setIClickPositionCallback(IClickPositionCallback mIClickPositionCallback) {
        this.mIClickPositionCallback = mIClickPositionCallback;
    }

    public interface IClickPositionCallback {
        void click(float x, float y);
    }

}
