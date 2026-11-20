package com.fotric.sdkdemo.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileErrorCode;
import com.fotric.irtekfilesdk.IrFileMarker;
import com.fotric.irtekfilesdk.IrFileMarkerNames;
import com.fotric.irtekfilesdk.IrFilePoint;
import com.fotric.irtekfilesdk.IrFileReference;
import com.fotric.irtekfilesdk.IrFileSize;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.adapter.ShapeParamAdapter;
import com.fotric.sdkdemo.adapter.ShapeRecyclerAdapter;
import com.fotric.sdkdemo.bean.FuncType;
import com.fotric.sdkdemo.bean.ShapeInfoItem;
import com.fotric.sdkdemo.bean.ShapeType;
import com.fotric.sdkdemo.bean.ThermalParamType;
import com.fotric.sdkdemo.utils.DensityUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ROIFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private RecyclerView mRvShapes, mRvInfos;
    private ShapeRecyclerAdapter mShapeRecyclerAdapter;
    private ShapeParamAdapter mShapeParamAdapter;
    private TextView mTvOpen;
    private boolean mOpened = false;
    private IRefreshCallback mIRefreshCallback;
    private int countPoint = 0, countLine = 0, countRect = 0, countCircle = 0;

    public ROIFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRefreshCallback = refreshCallback;
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_roi, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mRvShapes = mRootView.findViewById(R.id.rv_shapes);
        mRvInfos = mRootView.findViewById(R.id.rv_infos);
        mTvOpen = mRootView.findViewById(R.id.tv_open);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRvShapes.setLayoutManager(linearLayoutManager);
        mRvShapes.setAdapter(mShapeRecyclerAdapter = new ShapeRecyclerAdapter(getContext(), new ShapeRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ShapeType funcType) {
                switch (funcType) {
                    case Line:
                        addLine();
                        break;
                    case Point:
                        addPoint();
                        break;
                    case Rect:
                        addRect(true);
                        break;
                    case Circle:
                        addRect(false);
                        break;
                }
                buildItems();
            }

            private void addRect(boolean b) {
                IrFileMarker marker = new IrFileMarker();
                marker.name = ((b ? "R_" : "C_") + ((b ? countRect : countCircle) + 1)).getBytes(StandardCharsets.UTF_8);
                marker.type = b ? 3 : 4;
                marker.pointCount = 2;
                IrFilePoint pointStart = new IrFilePoint();
                IrFilePoint pointEnd = new IrFilePoint();
                IrFileSize thermalSize = mIRtekFile.getThermalSize();
                pointStart.x = (short) (thermalSize.width / 4);
                pointStart.y = (short) (thermalSize.height / 4);
                pointEnd.x = (short) (thermalSize.width * 3 / 4);
                pointEnd.y = (short) (thermalSize.height * 3 / 4);
                marker.points = new IrFilePoint[]{pointStart, pointEnd};
                mIRtekFile.setMarker(marker);
            }

            private void addLine() {
                IrFileMarker marker = new IrFileMarker();
                marker.name = ("L_" + (countLine + 1)).getBytes(StandardCharsets.UTF_8);
                marker.type = 6;
                marker.pointCount = 2;
                IrFilePoint pointStart = new IrFilePoint();
                IrFilePoint pointEnd = new IrFilePoint();
                IrFileSize thermalSize = mIRtekFile.getThermalSize();
                pointStart.x = (short) (thermalSize.width / 4);
                pointStart.y = (short) (thermalSize.height / 4);
                pointEnd.x = (short) (thermalSize.width * 3 / 4);
                pointEnd.y = (short) (thermalSize.height * 3 / 4);
                marker.points = new IrFilePoint[]{pointStart, pointEnd};
                mIRtekFile.setMarker(marker);
            }

            private void addPoint() {
                IrFileMarker marker = new IrFileMarker();
                marker.name = ("P_" + (countPoint + 1)).getBytes(StandardCharsets.UTF_8);
                marker.type = 1;
                marker.pointCount = 1;
                IrFilePoint point = new IrFilePoint();
                IrFileSize thermalSize = mIRtekFile.getThermalSize();
                point.x = (short) (thermalSize.width / 2);
                point.y = (short) (thermalSize.height / 2);
                marker.points = new IrFilePoint[]{point};
                mIRtekFile.setMarker(marker);
            }
        }));

        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(getContext());
        linearLayoutManager2.setOrientation(LinearLayoutManager.VERTICAL);
        mRvInfos.setLayoutManager(linearLayoutManager2);
        mRvInfos.setAdapter(mShapeParamAdapter = new ShapeParamAdapter(getActivity(), new ShapeParamAdapter.IThermalChangedCallback() {
            @Override
            public void delete(ShapeInfoItem infoItem) {
                IrFileErrorCode irFileErrorCode = mIRtekFile.removeMarker(infoItem.name.trim());
                if (irFileErrorCode.errorCode == IrFileErrorCode.IrFileEcOk) {
                    buildItems();
                }
            }
        }));

        mTvOpen.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                mOpened = !mOpened;
                mTvOpen.setText(mOpened ? "收起" : "展开");
                mRvShapes.setVisibility(!mOpened ? View.VISIBLE : View.GONE);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mRvInfos.getLayoutParams();
                layoutParams.height = mOpened ? DensityUtil.dp2px(600) : DensityUtil.dp2px(120);
                mRvInfos.setLayoutParams(layoutParams);
                mRvInfos.setBackgroundColor(getResources().getColor(mOpened ? R.color.black : android.R.color.transparent));
                mShapeParamAdapter.notifyDataSetChanged();
            }
        });
        buildItems();
    }

    private void buildItems() {
        ShapeType[] values = ShapeType.values();
        List<ShapeType> itemList = Arrays.asList(values);
        if (mShapeRecyclerAdapter != null) {
            mShapeRecyclerAdapter.replaceAll(itemList);
        }
        IrFileReference reference = mIRtekFile.getReference();
        boolean standard = reference.enabled == 0x01;
        IrFileMarkerNames markerNames = mIRtekFile.getMarkerNames();
        List<ShapeInfoItem> shapeInfoItems = new ArrayList<>();
        countCircle = 0;
        countLine = 0;
        countPoint = 0;
        countRect = 0;
        for (int i = 0; i < markerNames.count; i++) {
            IrFileMarker marker = mIRtekFile.getMarker(new String(markerNames.names[i], StandardCharsets.UTF_8).trim());
            if (marker.errorCode == IrFileErrorCode.IrFileEcOk) {
                ShapeInfoItem shapeInfoItem = new ShapeInfoItem();
                shapeInfoItem.type = marker.type;
                shapeInfoItem.averageValue = marker.tempValues[2];
                shapeInfoItem.name = new String(marker.name, StandardCharsets.UTF_8);
                shapeInfoItem.maxValue = marker.tempValues[0];
                shapeInfoItem.measuredValue = marker.measuredValue <= 0.0001 ? 0 : marker.measuredValue;
                shapeInfoItem.distance = marker.distance;
                shapeInfoItem.emissivity = marker.emissivity;
                shapeInfoItem.localParams = marker.localParams;
                shapeInfoItem.reflTemp = marker.reflTemp;
                shapeInfoItem.minValue = marker.tempValues[1];
                shapeInfoItem.enabledStandard = standard;
                shapeInfoItems.add(shapeInfoItem);
                switch (marker.type) {
                    case 1:
                        countPoint++;
                        break;
                    case 3:
                        countRect++;
                        break;
                    case 4:
                        countCircle++;
                        break;
                    case 6:
                        countLine++;
                        break;
                }
            }
        }
        Collections.reverse(shapeInfoItems);
        mShapeParamAdapter.replaceAll(shapeInfoItems);
        if (mIRefreshCallback != null) {
            mIRefreshCallback.refresh();
        }
    }
}
