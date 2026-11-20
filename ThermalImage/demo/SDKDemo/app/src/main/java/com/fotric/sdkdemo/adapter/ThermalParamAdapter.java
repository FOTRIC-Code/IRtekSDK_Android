package com.fotric.sdkdemo.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.bean.InfoItem;
import com.fotric.sdkdemo.bean.ThermalParamType;

import java.util.ArrayList;
import java.util.List;

public class ThermalParamAdapter extends RecyclerView.Adapter<ThermalParamAdapter.InfoItemViewHolder> {

    private List<InfoItem> infoItemList = new ArrayList<>();

    private Context mContext;
    private IThermalChangedCallback mIThermalChangedCallback;

    public ThermalParamAdapter(Context context, IThermalChangedCallback thermalChangedCallback) {
        this.mContext = context;
        mIThermalChangedCallback = thermalChangedCallback;
    }

    @NonNull
    @Override
    public InfoItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InfoItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_thermal_param_item, null));
    }

    @Override
    public void onBindViewHolder(@NonNull InfoItemViewHolder holder, int position) {
        InfoItem infoItem = infoItemList.get(position);
        holder.tvTitle.setText(infoItem.type.name);
        holder.tvValue.setText(infoItem.value);
        holder.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    if (mIThermalChangedCallback != null) {
                        String s = editable.toString();
                        if (!TextUtils.isEmpty(s)) {
                            mIThermalChangedCallback.changed(infoItem.type, Float.parseFloat(s));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void replaceAll(List<InfoItem> items) {
        infoItemList.clear();
        infoItemList.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return infoItemList.size();
    }

    public static class InfoItemViewHolder extends RecyclerView.ViewHolder {

        public TextView tvTitle, tvValue;

        public InfoItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvValue = itemView.findViewById(R.id.tv_value);
            tvValue.setEnabled(false);
        }
    }

    public interface IThermalChangedCallback {
        void changed(ThermalParamType type, float value);
    }
}
