package com.fotric.sdkdemo.utils;

import android.content.Context;
import android.graphics.drawable.VectorDrawable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

public class ViewHelper {

    public static void viewTouchState(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                try {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            view.setAlpha(0.5f);
                            if (view.getId() == -1) {
                                view.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.setAlpha(1f);
                                    }
                                }, 200);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            view.setAlpha(1f);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    public static void changeSVGColor(Context context, ImageView imageView, int resId, int color) {
        try {
            VectorDrawableCompat vectorDrawableCompat = VectorDrawableCompat.create(context.getResources(), resId, context.getTheme());
            if (vectorDrawableCompat == null) {
                return;
            }
            vectorDrawableCompat.setTint(context.getResources().getColor(color));
            imageView.setImageDrawable(vectorDrawableCompat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getScreenWidth(Context context) {
        WindowManager systemService = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        systemService.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }
}
