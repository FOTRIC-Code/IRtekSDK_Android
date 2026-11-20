package com.fotric.sdkdemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.sdkdemo.IRefreshCallback;
import com.fotric.sdkdemo.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class CommonFragment extends Fragment {

    private String[] mTables = {"测温参数", "属性信息", "备注信息", "用户信息"};
    private View mRootView;
    private IRtekFile mIRtekFile;
    private IRefreshCallback mRefreshCallback;
    private TabLayout mTlOrderTitle;
    private ViewPager mVpOrderContainer;
    private List<Fragment> mFragments = new ArrayList<>();

    public CommonFragment(IRtekFile irtekFile, IRefreshCallback refreshCallback) {
        super();
        mIRtekFile = irtekFile;
        mRefreshCallback = refreshCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_common_info, null);
        initView();
        initData();
        return mRootView;
    }

    private void initData() {
        mFragments.clear();
        mFragments.add(new ThermalParamFragment(mIRtekFile, mRefreshCallback));
        mFragments.add(new PropertiesFragment(mIRtekFile));
        mFragments.add(new NoteFragment(mIRtekFile));
        mFragments.add(new UserInfoFragment(mIRtekFile));
        TabLayoutAdapater fragmentAdater = new TabLayoutAdapater(getChildFragmentManager());
        mVpOrderContainer.setAdapter(fragmentAdater);
        mTlOrderTitle.setupWithViewPager(mVpOrderContainer);
    }

    private void initView() {
        mTlOrderTitle = mRootView.findViewById(R.id.tl_order_title);
        mVpOrderContainer = mRootView.findViewById(R.id.vp_order_container);
    }

    //自定义头部标签导航的容器
    class TabLayoutAdapater extends FragmentStatePagerAdapter {

        public TabLayoutAdapater(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTables.length;
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return mTables[position];
        }
    }
}
