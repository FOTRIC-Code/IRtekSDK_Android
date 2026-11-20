package com.fotric.sdkdemo.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileBuffer;
import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.utils.ViewHelper;

import java.nio.charset.StandardCharsets;

public class NoteFragment extends Fragment {

    private View mRootView;
    private IRtekFile mIRtekFile;
    private EditText mTvValue;
    private Button mBtnSave;

    public NoteFragment(IRtekFile irtekFile) {
        super();
        mIRtekFile = irtekFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_note, null);
        initView();
        return mRootView;
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        mTvValue = mRootView.findViewById(R.id.tv_value);
        mBtnSave = mRootView.findViewById(R.id.btn_save);
        IrFileBuffer note = mIRtekFile.getNote();
        mTvValue.setText(new String(note.buffer, StandardCharsets.UTF_8) + "");
        ViewHelper.viewTouchState(mBtnSave);
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String value = mTvValue.getText().toString();
                IrFileBuffer buffer = new IrFileBuffer();
                if (!TextUtils.isEmpty(value)) {
                    buffer.buffer = value.trim().getBytes(StandardCharsets.UTF_8);
                    buffer.length = buffer.buffer.length;
                    mIRtekFile.setNote(buffer);
                } else {
                    mIRtekFile.setNote(buffer);
                }
            }
        });
    }

}
