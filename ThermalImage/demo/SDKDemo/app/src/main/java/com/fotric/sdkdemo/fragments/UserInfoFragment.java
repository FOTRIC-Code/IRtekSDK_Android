package com.fotric.sdkdemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileErrorCode;
import com.fotric.irtekfilesdk.IrFileKeys;
import com.fotric.irtekfilesdk.IrFileKvPair;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.adapter.PropertiesAdapter;
import com.fotric.sdkdemo.bean.InfoItem;
import com.fotric.sdkdemo.view.UserItemDialog;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class UserInfoFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private RecyclerView mRvInfos;
    private PropertiesAdapter mAdapter;
    private TextView mTvAdd;
    private SimpleDateFormat mSdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
    private UserItemDialog mUserItemDialog;

    public UserInfoFragment(IRtekFile irtekFile) {
        super();
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_user_info, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mRvInfos = mRootView.findViewById(R.id.rv_infos);
        mTvAdd = mRootView.findViewById(R.id.tv_add);
        mUserItemDialog = new UserItemDialog(getContext(), new UserItemDialog.ISureCallback() {
            @Override
            public void add(String key, String value) {
                IrFileKvPair pair = new IrFileKvPair();
                pair.key = key.getBytes(StandardCharsets.UTF_8);
                pair.vBuffer = value.getBytes(StandardCharsets.UTF_8);
                pair.vLength = pair.vBuffer.length;
                mIRtekFile.setUserData(pair);
                buildData();
            }
        });
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRvInfos.setLayoutManager(linearLayoutManager);
        mRvInfos.setAdapter(mAdapter = new PropertiesAdapter(getContext()));
        buildData();
        mTvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUserItemDialog.show();
            }
        });
    }

    private void buildData() {
        List<InfoItem> infoItemList = new ArrayList<>();
        IrFileKeys keys = mIRtekFile.getKeys();
        String key = "";
        for (int i = 0; i < keys.count; i++) {
            key = new String(keys.keys[i], StandardCharsets.UTF_8);
            IrFileKvPair userData = mIRtekFile.getUserData(key.trim());
            if (userData.errorCode == IrFileErrorCode.IrFileEcOk) {
                infoItemList.add(new InfoItem(key, new String(userData.vBuffer, StandardCharsets.UTF_8)));
            }
        }
        mAdapter.replaceAll(infoItemList);
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
