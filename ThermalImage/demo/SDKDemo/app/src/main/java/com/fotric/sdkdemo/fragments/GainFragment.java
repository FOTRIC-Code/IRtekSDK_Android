package com.fotric.sdkdemo.fragments;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileErrorCode;
import com.fotric.irtekfilesdk.IrFileGain;
import com.fotric.irtekfilesdk.IrFilePoint;
import com.fotric.irtekfilesdk.IrFileSize;
import com.fotric.irtekfilesdk.IrFileTemp;
import com.fotric.irtekfilesdk.IrFileTempPoint;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.utils.DensityUtil;
import com.fotric.sdkdemo.utils.TempUtils;
import com.fotric.sdkdemo.view.MyOptionPicker;
import com.github.gzuliyujiang.dialog.DialogConfig;
import com.github.gzuliyujiang.dialog.DialogStyle;
import com.github.gzuliyujiang.wheelpicker.contract.OnOptionPickedListener;
import com.github.gzuliyujiang.wheelview.contract.OnWheelChangedListener;
import com.github.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

public class GainFragment extends Fragment {

    private View mRootView;
    private RadioGroup mViewModeGroup;
    private final IRtekFile mIRtekFile;
    private final IRefreshCallback mIRefreshCallback;
    private LinearLayout mllFinger;
    private TextView mEtTempMin, mEtTempMax;
    private boolean mIsFinger = false;

    public GainFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRtekFile = irtekFile;
        mIRefreshCallback = refreshCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_gain, null);
        initView();
        return mRootView;
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        mViewModeGroup = mRootView.findViewById(R.id.rg_mode);
        mllFinger = mRootView.findViewById(R.id.ll_finger);
        mEtTempMin = mRootView.findViewById(R.id.et_temp_min);
        mEtTempMax = mRootView.findViewById(R.id.et_temp_max);
        IrFileGain gain = mIRtekFile.getGain();
        if (gain.errorCode == IrFileErrorCode.IrFileEcOk) {
            mEtTempMin.setText(TempUtils.tempConvertKToC(gain.min) + "℃");
            mEtTempMax.setText(TempUtils.tempConvertKToC(gain.max) + "℃");
        }
        mViewModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (mIRtekFile != null) {
                    IrFileGain gain = new IrFileGain();
                    mIsFinger = false;
                    switch (i) {
                        case R.id.rb_auto:
                            gain.mode = 0x02;
                            gain.fixedSpan = 0x00;
                            break;
                        case R.id.rb_finger:
                            gain.mode = 0x03;
                            gain.fixedSpan = 0x01;
                            break;
                        case R.id.rb_self:
                            mIsFinger = true;
                            gain.mode = 0x02;
                            gain.fixedSpan = 0x01;
                            break;
                    }
                    mIRtekFile.setGain(gain);
                    mllFinger.setVisibility(i == R.id.rb_finger ? View.VISIBLE : View.GONE);
                    if (mIRefreshCallback != null) {
                        mIRefreshCallback.refresh();
                    }
                }
            }
        });
        mEtTempMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrFileGain gain = mIRtekFile.getGain();
                showPicker(gain.min - 10f, gain.max, gain.min, false);
            }
        });
        mEtTempMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrFileGain gain = mIRtekFile.getGain();
                showPicker(gain.min, gain.max + 10f, gain.max, true);
            }
        });
        DialogConfig.setDialogStyle(DialogStyle.Default);
    }

    /**
     * 伪造数据
     */
    private void showPicker(float min, float max, float defaultValue, boolean isMax) {
        List<Float> data = new ArrayList<>();
        Float defaultFloat = Float.valueOf(TempUtils.tempConvertKToC(defaultValue));
        for (float i = min; i <= max; i += 0.25) {
            if (i > defaultValue) {
                boolean contains = data.contains(defaultFloat);
                if (!contains) {
                    data.add(defaultFloat);
                }
            }
            data.add(Float.valueOf(TempUtils.tempConvertKToC(i)));
        }
        MyOptionPicker picker = new MyOptionPicker(getActivity(), R.style.PickerTrans);
        picker.setTitle(isMax ? "最大" : "最小" + "温度选择");
        picker.setBodyWidth(140);
        picker.setData(data);
        picker.setDefaultValue(Float.valueOf(TempUtils.tempConvertKToC(defaultValue)));
        ViewGroup.LayoutParams layoutParams = picker.getWheelLayout().getLayoutParams();
        layoutParams.height = DensityUtil.dp2px(150);
        picker.getWheelLayout().setLayoutParams(layoutParams);
        picker.setOnOptionPickedListener(new OnOptionPickedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onOptionPicked(int position, Object item) {

            }
        });
        picker.getWheelView().setOnWheelChangedListener(new OnWheelChangedListener() {

            @Override
            public void onWheelScrolled(WheelView view, int offset) {

            }

            @Override
            public void onWheelSelected(WheelView view, int position) {
                Float item = data.get(position);
                if (isMax) {
                    mEtTempMax.setText(item + "℃");
                } else {
                    mEtTempMin.setText(item + "℃");
                }
                IrFileGain gain = mIRtekFile.getGain();
                gain.min = Float.parseFloat(TempUtils.tempConvertCToK(Float.parseFloat(mEtTempMin.getText().toString().replaceAll("℃", ""))));
                gain.max = Float.parseFloat(TempUtils.tempConvertCToK(Float.parseFloat(mEtTempMax.getText().toString().replaceAll("℃", ""))));
                mIRtekFile.setGain(gain);
                if (mIRefreshCallback != null) {
                    mIRefreshCallback.refresh();
                }
            }

            @Override
            public void onWheelScrollStateChanged(WheelView view, int state) {

            }

            @Override
            public void onWheelLoopFinished(WheelView view) {

            }
        });
        picker.show();

    }

    public void setClickPosition(PointF pointF) {
        if (!mIsFinger) {
            return;
        }
        IrFileSize thermalSize = mIRtekFile.getThermalSize();
        IrFileTempPoint tempPoint = new IrFileTempPoint();
        IrFilePoint filePoint = new IrFilePoint();
        filePoint.x = (short) (pointF.x * thermalSize.width);
        filePoint.y = (short) (pointF.y * thermalSize.height);
        tempPoint.point = filePoint;
//        IrFileTempPointValue pointValue = mIRtekFile.getTempPoint(tempPoint);
        IrFileTemp pointValue = mIRtekFile.getTempSpot(tempPoint);
        IrFileGain gain = mIRtekFile.getGain();
        gain.min = pointValue.temp - 10f;
        gain.max = pointValue.temp + 10f;
        gain.mode = 0x03;
        gain.fixedSpan = 0x01;
        mIRtekFile.setGain(gain);
        if (mIRefreshCallback != null) {
            mIRefreshCallback.refresh();
        }
    }


    public interface IPositionCallback {
        PointF getClickPoint();
    }

}
