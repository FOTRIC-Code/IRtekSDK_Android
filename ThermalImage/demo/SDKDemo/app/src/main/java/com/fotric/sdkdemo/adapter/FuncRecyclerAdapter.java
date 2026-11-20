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
import com.fotric.sdkdemo.utils.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class FuncRecyclerAdapter extends RecyclerView.Adapter<FuncRecyclerAdapter.FuncItemViewHolder> {

    private List<FuncType> mItemList = new ArrayList<>();

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;
    private int mSelectedPosition = 0;

    public FuncRecyclerAdapter(Context context, OnItemClickListener onItemClickListener) {
        this.mContext = context;
        this.mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public FuncRecyclerAdapter.FuncItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item_func, null);
        return new FuncItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FuncRecyclerAdapter.FuncItemViewHolder holder, int position) {
        FuncType funcType = mItemList.get(position);
        holder.tvTitle.setText(funcType.title);
        holder.ivIcon.setImageResource(funcType.resId);
        holder.itemView.setTag(funcType);
        int color = (position == mSelectedPosition) ? R.color.bottom_select_color : R.color.white;
        ViewHelper.changeSVGColor(mContext, holder.ivIcon, funcType.resId, color);
        holder.tvTitle.setTextColor(mContext.getColor(color));
        ViewHelper.viewTouchState(holder.itemView);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                try {
                    if (mOnItemClickListener != null) {
                        FuncType func = (FuncType) view.getTag();
                        mSelectedPosition = func.ordinal();
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
    public void replaceAll(@NonNull List<FuncType> itemList) {
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
        void onItemClick(FuncType funcType);
    }
}
