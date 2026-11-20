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
import com.fotric.sdkdemo.bean.ShapeType;
import com.fotric.sdkdemo.utils.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class ShapeRecyclerAdapter extends RecyclerView.Adapter<ShapeRecyclerAdapter.FuncItemViewHolder> {

    private List<ShapeType> mItemList = new ArrayList<>();

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    public ShapeRecyclerAdapter(Context context, OnItemClickListener onItemClickListener) {
        this.mContext = context;
        this.mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ShapeRecyclerAdapter.FuncItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item_shape, null);
        return new FuncItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShapeRecyclerAdapter.FuncItemViewHolder holder, int position) {
        ShapeType funcType = mItemList.get(position);
        holder.tvTitle.setText(funcType.title);
        holder.ivIcon.setImageResource(funcType.resId);
        holder.itemView.setTag(funcType);
        ViewHelper.viewTouchState(holder.itemView);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                try {
                    if (mOnItemClickListener != null) {
                        ShapeType func = (ShapeType) view.getTag();
                        mOnItemClickListener.onItemClick(func);
                    }
                    notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceAll(@NonNull List<ShapeType> itemList) {
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
        void onItemClick(ShapeType funcType);
    }
}
