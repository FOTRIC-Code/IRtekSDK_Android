package com.fotric.sdkdemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.bean.InfoItem;

import java.util.ArrayList;
import java.util.List;

public class PropertiesAdapter extends RecyclerView.Adapter<PropertiesAdapter.InfoItemViewHolder> {

    private List<InfoItem> infoItemList = new ArrayList<>();

    private Context mContext;

    public PropertiesAdapter(Context context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public InfoItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InfoItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_common_item, null));
    }

    @Override
    public void onBindViewHolder(@NonNull InfoItemViewHolder holder, int position) {
        InfoItem infoItem = infoItemList.get(position);
        boolean isMainTitle = "10086".equals(infoItem.key);
        holder.tvTitleMain.setVisibility(isMainTitle ? View.VISIBLE : View.GONE);
        holder.llProperties.setVisibility(!isMainTitle ? View.VISIBLE : View.GONE);
        holder.tvTitleMain.setText(infoItem.value);
        holder.tvTitle.setText(infoItem.key);
        holder.tvValue.setText(infoItem.value);
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

        public TextView tvTitle, tvValue, tvTitleMain;
        public View llProperties;

        public InfoItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvValue = itemView.findViewById(R.id.tv_value);
            tvTitleMain = itemView.findViewById(R.id.tv_main_title);
            llProperties = itemView.findViewById(R.id.ll_properties);
        }
    }
}
