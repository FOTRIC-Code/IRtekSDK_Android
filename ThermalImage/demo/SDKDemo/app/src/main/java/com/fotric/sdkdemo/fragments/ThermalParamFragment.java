package com.fotric.sdkdemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileThermalParams;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.adapter.ThermalParamAdapter;
import com.fotric.sdkdemo.bean.InfoItem;
import com.fotric.sdkdemo.bean.ThermalParamType;
import com.fotric.sdkdemo.utils.TempUtils;

import java.util.ArrayList;
import java.util.List;

public class ThermalParamFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private IRefreshCallback mIRefreshCallback;
    private RecyclerView mRvInfos;
    private ThermalParamAdapter mAdapter;

    public ThermalParamFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRefreshCallback = refreshCallback;
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_thremal_param, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mRvInfos = mRootView.findViewById(R.id.rv_infos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRvInfos.setLayoutManager(linearLayoutManager);
        mRvInfos.setAdapter(mAdapter = new ThermalParamAdapter(getContext(), new ThermalParamAdapter.IThermalChangedCallback() {
            @Override
            public void changed(ThermalParamType type, float value) {
                IrFileThermalParams thermalParams = mIRtekFile.getThermalParams();
                switch (type) {
                    case atmTemp:
                        thermalParams.atmTemp = value;
                        break;
                    case distance:
                        break;
                    case emissivity:
                        break;
                    case extOptTemp:
                        break;
                    case extOptTrans:
                        break;
                    case reflTemp:
                        break;
                    case relHumidity:
                        break;
                }
                mIRtekFile.setThermalParams(thermalParams);
                if (mIRefreshCallback != null){
                    mIRefreshCallback.refresh();
                }
            }
        }));
        buildItems();
    }

    private void buildItems() {
        List<InfoItem> infoItemList = new ArrayList<>();
        IrFileThermalParams thermalParams = mIRtekFile.getThermalParams();
        infoItemList.add(new InfoItem(ThermalParamType.atmTemp, TempUtils.tempConvertKToC(thermalParams.atmTemp) + "℃"));
        infoItemList.add(new InfoItem(ThermalParamType.distance, thermalParams.distance + ""));
        infoItemList.add(new InfoItem(ThermalParamType.emissivity, thermalParams.emissivity + ""));
        infoItemList.add(new InfoItem(ThermalParamType.extOptTemp, TempUtils.tempConvertKToC(thermalParams.extOptTemp) + "℃"));
        infoItemList.add(new InfoItem(ThermalParamType.extOptTrans, thermalParams.extOptTrans + ""));
        infoItemList.add(new InfoItem(ThermalParamType.reflTemp, TempUtils.tempConvertKToC(thermalParams.reflTemp) + "℃"));
        infoItemList.add(new InfoItem(ThermalParamType.relHumidity, thermalParams.relHumidity + ""));
        mAdapter.replaceAll(infoItemList);
    }

}
