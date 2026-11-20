package com.fotric.sdkdemo.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileAlarm;
import com.fotric.irtekfilesdk.IrFileErrorCode;
import com.fotric.irtekfilesdk.IrFileGain;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.utils.DensityUtil;
import com.fotric.sdkdemo.utils.TempUtils;
import com.fotric.sdkdemo.utils.ViewHelper;
import com.fotric.sdkdemo.view.MyColorPicker;
import com.fotric.sdkdemo.view.MyOptionPicker;
import com.github.gzuliyujiang.colorpicker.ColorGradientView;
import com.github.gzuliyujiang.colorpicker.ColorPicker;
import com.github.gzuliyujiang.colorpicker.OnColorPickedListener;
import com.github.gzuliyujiang.wheelpicker.contract.OnOptionPickedListener;
import com.github.gzuliyujiang.wheelview.contract.OnWheelChangedListener;
import com.github.gzuliyujiang.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.List;

public class AlarmFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private TextView mTvAlarmType, mTvTempMin, mTvTempMax, mTvAlarmColor;
    private View mLlAlarmMin, mLlAlarmMax;
    private MyOptionPicker mTypePicker;
    private IRefreshCallback mIRefreshCallback;
    private ColorPicker mColorPicker;

    public AlarmFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRefreshCallback = refreshCallback;
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_alarm, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mTvAlarmType = mRootView.findViewById(R.id.tv_alarm_type);
        mLlAlarmMin = mRootView.findViewById(R.id.ll_alarm_min);
        mLlAlarmMax = mRootView.findViewById(R.id.ll_alarm_max);
        mTvTempMin = mRootView.findViewById(R.id.tv_temp_min);
        mTvTempMax = mRootView.findViewById(R.id.tv_temp_max);
        mTvAlarmColor = mRootView.findViewById(R.id.tv_alarm_color);
        ViewHelper.viewTouchState(mTvAlarmType);
        ViewHelper.viewTouchState(mTvTempMin);
        ViewHelper.viewTouchState(mTvTempMax);
        ViewHelper.viewTouchState(mTvAlarmColor);
        mTvAlarmType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                mTypePicker.setDefaultPosition(alarm.type);
                mTypePicker.show();
            }
        });
        mTypePicker = new MyOptionPicker(getActivity(), R.style.PickerTrans);
        mTypePicker.setTitle("报警类型");
        mTypePicker.setBodyWidth(140);
        ViewGroup.LayoutParams layoutParams = mTypePicker.getWheelLayout().getLayoutParams();
        layoutParams.height = DensityUtil.dp2px(150);
        mTypePicker.getWheelLayout().setLayoutParams(layoutParams);
        mTypePicker.setOnOptionPickedListener(new OnOptionPickedListener() {
            @Override
            public void onOptionPicked(int position, Object item) {
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                alarm.type = ((AlarmType) item).type;
                if (alarm.type != 0 && alarm.type != 4) {
                    alarm.custom = 0x01;
                } else {
                    alarm.custom = 0x00;
                }
                mIRtekFile.setAlarm(alarm);
                initView();
                if (mIRefreshCallback != null) {
                    mIRefreshCallback.refresh();
                }
            }
        });
        mColorPicker = new MyColorPicker(getActivity(), R.style.PickerTrans){
            @Override
            public void onColorChanged(ColorGradientView gradientView, int color) {
                super.onColorChanged(gradientView, color);

            }
        };
        mColorPicker.setOnColorPickListener(new OnColorPickedListener() {
            @Override
            public void onColorPicked(int color) {
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                alarm.color = color;
                mIRtekFile.setAlarm(alarm);
                mTvAlarmColor.setBackgroundColor(color);
                if (mIRefreshCallback != null) {
                    mIRefreshCallback.refresh();
                }
            }
        });
        mTvTempMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                showPicker(alarm.min - 10f, alarm.max, alarm.min, false);
            }
        });
        mTvTempMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                showPicker(alarm.min, alarm.max + 10f, alarm.max, true);
            }
        });
        mTvAlarmColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                mColorPicker.setInitColor(alarm.color);
                mColorPicker.show();
            }
        });
        buildData();
    }

    @SuppressLint("SetTextI18n")
    private void buildData() {
        IrFileAlarm alarm = mIRtekFile.getAlarm();

        List<AlarmType> data = new ArrayList<>();
        data.add(new AlarmType(0, "不启用报警"));
        data.add(new AlarmType(1, "低于报警"));
        data.add(new AlarmType(2, "高于报警"));
        data.add(new AlarmType(3, "之间报警"));
        data.add(new AlarmType(4, "MagicThermal报警"));
        data.add(new AlarmType(5, "之外报警"));
        data.add(new AlarmType(6, "湿度报警"));
        data.add(new AlarmType(7, "保温报警"));

        if (alarm.errorCode == IrFileErrorCode.IrFileEcOk) {
            int type = alarm.type;
            mTvAlarmType.setText(getAlarmTypeNameByType(type));
            mTvTempMin.setText(TempUtils.tempConvertKToC(alarm.min) + "℃");
            mTvTempMax.setText(TempUtils.tempConvertKToC(alarm.max) + "℃");
            mTvAlarmColor.setBackgroundColor(alarm.color);
            mTypePicker.setDefaultPosition(alarm.type);
            mTypePicker.setData(data);

            mColorPicker.setInitColor(alarm.color);

            boolean formatMax = alarm.type == 2 || alarm.type == 3 || alarm.type == 5;
            boolean formatMin = alarm.type == 1 || alarm.type == 3 || alarm.type == 5;
            mLlAlarmMax.setAlpha(formatMax ? 1f : 0.5f);
            mLlAlarmMin.setAlpha(formatMin ? 1f : 0.5f);

            mTvTempMin.setEnabled(formatMin);
            mLlAlarmMax.setEnabled(formatMax);
        }


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

            @SuppressLint("SetTextI18n")
            @Override
            public void onWheelSelected(WheelView view, int position) {
                Float item = data.get(position);
                if (isMax) {
                    mTvTempMax.setText(item + "℃");
                } else {
                    mTvTempMin.setText(item + "℃");
                }
                IrFileAlarm alarm = mIRtekFile.getAlarm();
                alarm.min = Float.parseFloat(TempUtils.tempConvertCToK(Float.parseFloat(mTvTempMin.getText().toString().replaceAll("℃", ""))));
                alarm.max = Float.parseFloat(TempUtils.tempConvertCToK(Float.parseFloat(mTvTempMax.getText().toString().replaceAll("℃", ""))));
                mIRtekFile.setAlarm(alarm);
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

    /**
     * 报警类型，默认为不启用 1:不启用报警 2:低于报警 3:高于报警 4:之间报警 5:MagicThermal报警 6:之外报警 7:湿度报警 8:保温报警
     *
     * @param type
     * @return
     */
    private String getAlarmTypeNameByType(int type) {
        switch (type) {
            case 0:
            default:
                return "不启用报警";
            case 1:
                return "低于报警";
            case 2:
                return "高于报警";
            case 3:
                return "之间报警";
            case 4:
                return "MagicThermal报警";
            case 5:
                return "之外报警";
            case 6:
                return "湿度报警";
            case 7:
                return "保温报警";
        }
    }

    public static class AlarmType {
        public int type;
        public String name;

        public AlarmType(int type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
