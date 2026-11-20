package com.fotric.sdkdemo.adapter;

import static java.lang.String.format;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.bean.ShapeInfoItem;
import com.fotric.sdkdemo.bean.ThermalParamType;
import com.fotric.sdkdemo.utils.TempUtils;
import com.fotric.sdkdemo.utils.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class ShapeParamAdapter extends RecyclerView.Adapter<ShapeParamAdapter.InfoItemViewHolder> {

    private List<ShapeInfoItem> infoItemList = new ArrayList<>();

    private Context mContext;
    private IThermalChangedCallback mIThermalChangedCallback;

    public ShapeParamAdapter(Context context, IThermalChangedCallback thermalChangedCallback) {
        this.mContext = context;
        mIThermalChangedCallback = thermalChangedCallback;
    }

    @NonNull
    @Override
    public InfoItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InfoItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_shape_param_item, null));
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull InfoItemViewHolder holder, int position) {
        ShapeInfoItem infoItem = infoItemList.get(position);
        holder.tvName.setText(infoItem.name);
        holder.tvTempMax.setText((infoItem.enabledStandard ? format("%.2f", infoItem.maxValue) : TempUtils.tempConvertKToC(infoItem.maxValue)) + "℃");
        holder.tvTempMin.setText((infoItem.enabledStandard ? format("%.2f", infoItem.minValue) : TempUtils.tempConvertKToC(infoItem.minValue)) + "℃");
        holder.tvTempAverage.setText((infoItem.enabledStandard ? format("%.2f", infoItem.averageValue) : TempUtils.tempConvertKToC(infoItem.averageValue)) + "℃");
        holder.tvMearsure.setText(format("%.2f", infoItem.measuredValue) + "");
        holder.tvEnable.setText(infoItem.localParams == 0x01 ? "启用" : "未启用");
        holder.tvEmissivity.setText(infoItem.emissivity + "");
        holder.tvDistance.setText(infoItem.distance + "");
        holder.tvReflTemp.setText(TempUtils.tempConvertKToC(infoItem.reflTemp) + "℃");
        ViewHelper.viewTouchState(holder.tvDelete);
        holder.tvDelete.setTag(infoItem);
        holder.tvDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIThermalChangedCallback != null) {
                    mIThermalChangedCallback.delete((ShapeInfoItem) view.getTag());
                }
            }
        });
        if ("global".equals(infoItem.name.trim())) {
            holder.tvDelete.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDelete.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceAll(List<ShapeInfoItem> items) {
        infoItemList.clear();
        infoItemList.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return infoItemList.size();
    }

    public static class InfoItemViewHolder extends RecyclerView.ViewHolder {

        public TextView tvName, tvTempMax, tvTempMin, tvTempAverage, tvMearsure, tvEnable, tvEmissivity, tvReflTemp, tvDistance;
        public TextView tvDelete;

        public InfoItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDelete = itemView.findViewById(R.id.tv_delete);
            tvTempMax = itemView.findViewById(R.id.tv_temp_max);
            tvTempMin = itemView.findViewById(R.id.tv_temp_min);
            tvTempAverage = itemView.findViewById(R.id.tv_temp_average);
            tvMearsure = itemView.findViewById(R.id.tv_mearsure);
            tvEnable = itemView.findViewById(R.id.tv_enable);
            tvEmissivity = itemView.findViewById(R.id.tv_emissivity);
            tvReflTemp = itemView.findViewById(R.id.tv_refl_temp);
            tvDistance = itemView.findViewById(R.id.tv_distance);
        }
    }

    public interface IThermalChangedCallback {
        void delete(ShapeInfoItem infoItem);
    }
}
