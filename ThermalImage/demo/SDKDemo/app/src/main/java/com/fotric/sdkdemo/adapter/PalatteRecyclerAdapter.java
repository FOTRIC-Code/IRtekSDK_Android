package com.fotric.sdkdemo.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.sdkdemo.R;
import com.fotric.sdkdemo.bean.FuncType;
import com.fotric.sdkdemo.bean.PalatteInfoItem;
import com.fotric.sdkdemo.utils.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class PalatteRecyclerAdapter extends RecyclerView.Adapter<PalatteRecyclerAdapter.FuncItemViewHolder> {

    private List<PalatteInfoItem> mItemList = new ArrayList<>();

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    public PalatteRecyclerAdapter(Context context, OnItemClickListener onItemClickListener) {
        this.mContext = context;
        this.mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public PalatteRecyclerAdapter.FuncItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item_palatte, parent, false);
        return new FuncItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PalatteRecyclerAdapter.FuncItemViewHolder holder, int position) {
        PalatteInfoItem palatteInfoItem = mItemList.get(position);
        holder.tvTitle.setText(palatteInfoItem.key);
        holder.ivIcon.setImageBitmap(palatteInfoItem.image);
        holder.tvTitle.setTextColor(mContext.getResources().getColor(palatteInfoItem.selected ? R.color.bottom_select_color : R.color.white));
        holder.itemView.setTag(palatteInfoItem);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                resetDefaultSelected();
                PalatteInfoItem infoItem = (PalatteInfoItem) view.getTag();
                infoItem.selected = true;
                notifyDataSetChanged();
                if (mOnItemClickListener != null){
                    mOnItemClickListener.onItemClick(infoItem);
                }
            }
        });
    }

    private void resetDefaultSelected() {
        for (PalatteInfoItem item : mItemList) {
            item.selected = false;
        }
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceAll(@NonNull List<PalatteInfoItem> itemList) {
        mItemList.clear();
        mItemList.addAll(itemList);
        notifyDataSetChanged();
    }

    public static class FuncItemViewHolder extends RecyclerView.ViewHolder {

        public ImageView ivIcon;
        public TextView tvTitle;

        public FuncItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PalatteInfoItem item);
    }
}
