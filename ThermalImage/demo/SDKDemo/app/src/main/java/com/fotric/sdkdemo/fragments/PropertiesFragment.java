package com.fotric.sdkdemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileBuffer;
import com.fotric.irtekfilesdk.IrFileCameraInfo;
import com.fotric.irtekfilesdk.IrFileGeoLocation;
import com.fotric.irtekfilesdk.IrFileLensInfo;
import com.fotric.irtekfilesdk.IrFileMappingInfo;
import com.fotric.irtekfilesdk.IrFileSize;
import com.fotric.irtekfilesdk.IrFileTempRange;
import com.fotric.irtekfilesdk.IrFileVersion;
import com.fotric.irtekfilesdk.IrFileZoomInfo;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.adapter.PropertiesAdapter;
import com.fotric.sdkdemo.bean.InfoItem;
import com.fotric.sdkdemo.utils.DensityUtil;
import com.fotric.sdkdemo.utils.TempUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class PropertiesFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private RecyclerView mRvInfos;
    private PropertiesAdapter mAdapter;
    private TextView mTvOpen;
    private boolean mOpened = false;
    private SimpleDateFormat mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PropertiesFragment(IRtekFile irtekFile) {
        super();
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_propertice, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mRvInfos = mRootView.findViewById(R.id.rv_infos);
        mTvOpen = mRootView.findViewById(R.id.tv_open);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRvInfos.setLayoutManager(linearLayoutManager);
        mRvInfos.setAdapter(mAdapter = new PropertiesAdapter(getContext()));
        buildData();
        mTvOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOpened = !mOpened;
                mTvOpen.setText(mOpened ? "收起" : "展开");
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mRvInfos.getLayoutParams();
                layoutParams.height = mOpened ? DensityUtil.dp2px(600) : DensityUtil.dp2px(120);
                mRvInfos.setLayoutParams(layoutParams);
                mRvInfos.setBackgroundColor(getResources().getColor(mOpened ? R.color.black : android.R.color.transparent));
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void buildData() {
        List<InfoItem> infoItemList = new ArrayList<>();
        infoItemList.add(new InfoItem("10086", "设备信息"));
        IrFileCameraInfo cameraInfo = mIRtekFile.getCameraInfo();
        infoItemList.add(new InfoItem("设备型号", new String(cameraInfo.model, StandardCharsets.UTF_8)));
        infoItemList.add(new InfoItem("序列号", new String(cameraInfo.serialNo, StandardCharsets.UTF_8)));
        infoItemList.add(new InfoItem("公司", new String(cameraInfo.company, StandardCharsets.UTF_8)));
        infoItemList.add(new InfoItem("品牌", new String(cameraInfo.brand, StandardCharsets.UTF_8)));

        IrFileVersion sdkVersion = mIRtekFile.getSDKVersion();
        infoItemList.add(new InfoItem("版本号", new String(sdkVersion.buffer, StandardCharsets.UTF_8)));
        IrFileVersion fileVersion = mIRtekFile.getFileVersion();
        infoItemList.add(new InfoItem("文件版本", new String(fileVersion.buffer, StandardCharsets.UTF_8)));

        infoItemList.add(new InfoItem("10086", "镜头信息"));
        IrFileLensInfo lensInfo = mIRtekFile.getLensInfo();
        infoItemList.add(new InfoItem("镜头名", new String(lensInfo.name, StandardCharsets.UTF_8)));
        infoItemList.add(new InfoItem("镜头焦距", lensInfo.focalLength + ""));
        infoItemList.add(new InfoItem("镜头度数", lensInfo.hfov + ""));

        infoItemList.add(new InfoItem("10086", "测量区间"));
        IrFileTempRange tempRange = mIRtekFile.getTempRange();
        infoItemList.add(new InfoItem("最大温度", TempUtils.tempConvertKToC(tempRange.max) + "℃"));
        infoItemList.add(new InfoItem("最小温度", TempUtils.tempConvertKToC(tempRange.min) + "℃"));

        infoItemList.add(new InfoItem("10086", "图像大小"));
        IrFileSize thermalSize = mIRtekFile.getThermalSize();
        infoItemList.add(new InfoItem("热像图", thermalSize.width + " x " + thermalSize.height));
        IrFileBuffer visualImage = mIRtekFile.getVisualImage();
        IrFileMappingInfo mappingInfo = mIRtekFile.getMappingInfo();
        infoItemList.add(new InfoItem("可见光图", mappingInfo.width + " x " + mappingInfo.height));
        infoItemList.add(new InfoItem("热像区域", "(" + mappingInfo.rectX + ", " + mappingInfo.rectY + ", " + mappingInfo.rectWidth + ", " + mappingInfo.rectHeight + ")"));

        infoItemList.add(new InfoItem("10086", "数码变焦信息"));
        IrFileZoomInfo zoomInfo = mIRtekFile.getZoomInfo();
        infoItemList.add(new InfoItem("变焦信息", zoomInfo.enabled == 0x01 ? "启用" : "未启用"));
        infoItemList.add(new InfoItem("倍数", zoomInfo.ratio + ""));
        infoItemList.add(new InfoItem("参考坐标", "(" + zoomInfo.x + ", " + zoomInfo.y + ")"));

        infoItemList.add(new InfoItem("10086", "位置信息"));
        IrFileGeoLocation geoLocation = mIRtekFile.getGeoLocation();
        infoItemList.add(new InfoItem("GPS", geoLocation.isGPSValid == 0x01 ? "可用" : "不可用"));
        infoItemList.add(new InfoItem("经度", geoLocation.longitude + ""));
        infoItemList.add(new InfoItem("维度", geoLocation.latitude + ""));
        infoItemList.add(new InfoItem("海拔", geoLocation.altitude + ""));
        infoItemList.add(new InfoItem("方向", geoLocation.direction + "（方向 取值为-180~180的角度，+-180表示正南方向）"));
        infoItemList.add(new InfoItem("指南针", geoLocation.isCompassValid == 0x01 ? "可用" : "不可用"));

        infoItemList.add(new InfoItem("10086", "文件信息"));
        infoItemList.add(new InfoItem("文件路径", new String(mIRtekFile.getFileName().buffer, StandardCharsets.UTF_8)));
        infoItemList.add(new InfoItem("文件类型", getFileTypeName(mIRtekFile.getFileType().curType)));
        infoItemList.add(new InfoItem("拍摄时间", mSdf.format(new Date(mIRtekFile.getShotTime().time))));
        infoItemList.add(new InfoItem("修改时间", mSdf.format(new Date(mIRtekFile.getModifiedTime().time))));
        mAdapter.replaceAll(infoItemList);
    }

    /**
     * 获取0时区时间
     */
    public static String formatToTimeZone(long time, String timeZoneKey, String pattern) {
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneKey);
        SimpleDateFormat gmtData = new SimpleDateFormat(pattern); // 东京
        gmtData.setTimeZone(timeZone);
        return gmtData.format(new Date(time));
    }

    public String getFileTypeName(int type) {
        switch (type) {
            case 0:
                return "非热像文件";
            case 1:
                return "热像图片";
            case 2:
                return "热像视频";
            case 3:
                return "可见光图片";
            case 4:
                return " 电网热像图片";
        }
        return "";
    }

}
