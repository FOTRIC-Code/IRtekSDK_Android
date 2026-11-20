package com.fotric.sdkdemo.fragments;

import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileImage;
import com.fotric.irtekfilesdk.IrFilePalette;
import com.fotric.irtekfilesdk.IrFilePresetPalette;
import com.fotric.irtekfilesdk.IrFilePresetPalettes;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.adapter.PalatteRecyclerAdapter;
import com.fotric.sdkdemo.bean.FuncType;
import com.fotric.sdkdemo.bean.PalatteInfoItem;
import com.fotric.sdkdemo.utils.IRtekBitmapUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PalatteFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private RecyclerView mRvPalattes;
    private PalatteRecyclerAdapter mAdapter;
    private IRefreshCallback mIRefreshCallback;
    private CheckBox mCbInvert;

    public PalatteFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRefreshCallback = refreshCallback;
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_palatte, null);
        initView();
        return mRootView;
    }

    private void initView() {
        mRvPalattes = mRootView.findViewById(R.id.rv_palattes);
        mCbInvert = mRootView.findViewById(R.id.cb_invert);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRvPalattes.setLayoutManager(linearLayoutManager);
        mRvPalattes.setAdapter(mAdapter = new PalatteRecyclerAdapter(getContext(), new PalatteRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PalatteInfoItem item) {
                IrFilePalette filePalette = mIRtekFile.getPalette();
                filePalette.refNo = item.refNo;
                mIRtekFile.setPalette(filePalette);
                if (mIRefreshCallback != null) {
                    mIRefreshCallback.refresh();
                }
            }
        }));
        mCbInvert.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                IrFilePalette filePalette = mIRtekFile.getPalette();
                filePalette.inverted = (byte) (b ? 0x01 : 0x00);
                mIRtekFile.setPalette(filePalette);
                if (mIRefreshCallback != null) {
                    mIRefreshCallback.refresh();
                }
            }
        });
        buildData();
    }

    private void buildData() {
        List<PalatteInfoItem> palatteInfoItemList = new ArrayList<>();
        IrFilePresetPalettes presetPalettes = mIRtekFile.getPresetPalettes();
        IrFilePalette filePalette = mIRtekFile.getPalette();
        mCbInvert.setChecked(filePalette.inverted == 0x01);
        if (presetPalettes.count > 0) {
            IrFilePresetPalette[] palettes = presetPalettes.presetPalettes;
            for (IrFilePresetPalette palette : palettes) {
                IrFileImage presetPaletteImage = mIRtekFile.getPresetPaletteImage(palette.refNo, 0);
                PalatteInfoItem palatteInfoItem = new PalatteInfoItem(new String(palette.name, StandardCharsets.UTF_8), palette.refNo, IRtekBitmapUtils.getBitmapFromIRtekImage(presetPaletteImage, new Size(1, 256)));
                if (filePalette.refNo == palette.refNo) {
                    palatteInfoItem.selected = true;
                }
                palatteInfoItemList.add(palatteInfoItem);
            }
        }
        mAdapter.replaceAll(palatteInfoItemList);

    }

}
