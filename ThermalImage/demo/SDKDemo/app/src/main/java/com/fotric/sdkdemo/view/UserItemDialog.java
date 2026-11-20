package com.fotric.sdkdemo.view;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fotric.sdkdemo.R;

public class UserItemDialog extends Dialog {

    private EditText tvValue, tvKey;
    private Button btnCancel, btnOk;
    private ISureCallback mSureCallback;

    public UserItemDialog(@NonNull Context context, ISureCallback sureCallback) {
        super(context);
        mSureCallback = sureCallback;
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_user_item_layout, null);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnOk = view.findViewById(R.id.btn_ok);
        tvKey = view.findViewById(R.id.tv_key);
        tvValue = view.findViewById(R.id.tv_value);
        setCancelable(false);
        setContentView(view);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String key = tvKey.getText().toString();
                String value = tvValue.getText().toString();
                if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                    Toast.makeText(getOwnerActivity(), "键或值不能为空", Toast.LENGTH_LONG).show();
                    return;
                }
                if (mSureCallback != null) {
                    mSureCallback.add(key, value);
                    dismiss();
                    tvKey.setText("");
                    tvValue.setText("");
                }
            }
        });
    }

    public UserItemDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected UserItemDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public interface ISureCallback {
        void add(String key, String value);
    }

}
