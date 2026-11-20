package com.fotric.sdkdemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileSize;
import com.fotric.irtekfilesdk.IrFileViewInfo;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;

public class ImageFragment extends Fragment {

    private View mRootView;
    private RadioGroup mViewModeGroup;
    private final IRtekFile mIRtekFile;
    private final IRefreshCallback mIRefreshCallback;

    public ImageFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRtekFile = irtekFile;
        mIRefreshCallback = refreshCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_image, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mViewModeGroup = mRootView.findViewById(R.id.rg_mode);
        mViewModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (mIRtekFile != null) {
                    IrFileViewInfo viewInfo = new IrFileViewInfo();
                    switch (i) {
                        case R.id.rb_ir:
                            viewInfo.type = 0x01;
                            viewInfo.x = 0f;
                            viewInfo.y = 0f;
                            viewInfo.width = 1f;
                            viewInfo.height = 1f;
                            break;
                        case R.id.rb_visual:
                            viewInfo.type = 0x03;
                            viewInfo.alpha = 0x00;
                            break;
                        case R.id.rb_pinp:
                            viewInfo.alpha = 100;
                            viewInfo.type = 0x02;
                            viewInfo.x = 0.25f;
                            viewInfo.y = 0.25f;
                            viewInfo.width = 0.5f;
                            viewInfo.height = 0.5f;
                            break;
                        case R.id.rb_blend:
                            viewInfo.type = 0x03;
                            viewInfo.alpha = 50;
                            break;
                    }
                    mIRtekFile.setViewInfo(viewInfo);
                    if (mIRefreshCallback != null) {
                        mIRefreshCallback.refresh();
                    }
                }
            }
        });
    }

}
