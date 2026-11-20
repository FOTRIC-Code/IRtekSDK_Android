package com.fotric.sdkdemo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileErrorCode;
import com.fotric.irtekfilesdk.IrFileImage;
import com.fotric.irtekfilesdk.IrFileMarker;
import com.fotric.irtekfilesdk.IrFileMarkerNames;
import com.fotric.irtekfilesdk.IrFilePoint;
import com.fotric.irtekfilesdk.IrFileSize;
import com.fotric.sdkdemo.adapter.FuncRecyclerAdapter;
import com.fotric.sdkdemo.bean.FuncType;
import com.fotric.sdkdemo.bean.ShapeType;
import com.fotric.sdkdemo.fragments.AlarmFragment;
import com.fotric.sdkdemo.fragments.CommonFragment;
import com.fotric.sdkdemo.fragments.GainFragment;
import com.fotric.sdkdemo.fragments.ImageFragment;
import com.fotric.sdkdemo.fragments.PalatteFragment;
import com.fotric.sdkdemo.fragments.ROIFragment;
import com.fotric.sdkdemo.utils.IRtekBitmapUtils;
import com.fotric.sdkdemo.utils.ThreadManager;
import com.fotric.sdkdemo.utils.ViewHelper;
import com.fotric.sdkdemo.view.ParentIRView;
import com.fotric.sdkdemo.view.ShapeView;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class MainActivity extends FragmentActivity implements IRefreshCallback, ParentIRView.IClickPositionCallback {

    public static final String TAG_FILE_PATH = "tag_file_path";

    private RecyclerView mRvFuncs;
    private FuncRecyclerAdapter mAdapter;
    private ImageView mIVIrtek;
    private IRtekFile mIRtekFile;
    private String mFilePath;
    private ParentIRView mPvIRtek;
    private RelativeLayout mRlRois;
    private boolean mHasRelativeViewPosition = false;
    private int mScreenWidth;
    private ImageView mIvClose, mIvSave, mIvParams;
    private LinkedHashMap<FuncType, Fragment> mFuncFragment = new LinkedHashMap<>();
    private Bitmap mThermalBitmap;
    private IrFileSize mThermalSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFilePath = getIntent().getStringExtra(TAG_FILE_PATH);
        mScreenWidth = ViewHelper.getScreenWidth(this);
        initView();
        initIRtekFile();
        initAdapter();
        buildItems();
    }

    /**
     * 打开热像文件
     */
    private void initIRtekFile() {
        ThreadManager.getInstance().postNetIO(new Runnable() {
            @Override
            public void run() {
                mIRtekFile = new IRtekFile(mFilePath);
                initFragments();
                refreshThermalImage();
            }
        });
    }

    /**
     * 刷新热像图
     */
    private void refreshThermalImage() {
        if (mIRtekFile != null) {
            ThreadManager.getInstance().postNetIO(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    System.out.println("time = " + (System.currentTimeMillis() - startTime));
                    IrFileImage thermalImage = mIRtekFile.getThermalImage(0);
                    System.out.println("time 1= " + (System.currentTimeMillis() - startTime));
                    if (mThermalSize == null) {
                        mThermalSize = mIRtekFile.getThermalSize();
                    }
                    mThermalBitmap = IRtekBitmapUtils.getBitmapFromIRtekImage(thermalImage, new Size(mThermalSize.width, mThermalSize.height));
                    System.out.println("time 2= " + (System.currentTimeMillis() - startTime));
                    ThreadManager.getInstance().removeRunnable(mRefreshRunnable);
                    ThreadManager.getInstance().postToMainHandler(mRefreshRunnable, 50);
                }
            });
        }
    }

    private void initAdapter() {
        GridLayoutManager linearLayoutManager = new GridLayoutManager(this, 6);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRvFuncs.setLayoutManager(linearLayoutManager);
        mRvFuncs.setAdapter(mAdapter = new FuncRecyclerAdapter(this, new FuncRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FuncType funcType) {
                showFragmentByType(funcType);
            }
        }));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mRvFuncs = findViewById(R.id.rv_funcs);
        mIVIrtek = findViewById(R.id.iv_irtek);
        mPvIRtek = findViewById(R.id.pv_irtek);
        mIvClose = findViewById(R.id.iv_close);
        mIvSave = findViewById(R.id.iv_save);
        mIvParams = findViewById(R.id.iv_params);
        mRlRois = findViewById(R.id.rl_rois);
        ViewHelper.viewTouchState(mIvClose);
        ViewHelper.viewTouchState(mIvSave);
        ViewHelper.viewTouchState(mIvParams);
        mPvIRtek.setIClickPositionCallback(this);
        mIvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mIvSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadManager.getInstance().postNetIO(new Runnable() {
                    @Override
                    public void run() {
                        if (mIRtekFile != null) {
                            IrFileErrorCode irFileErrorCode = mIRtekFile.save();
                            ThreadManager.getInstance().postToMainHandler(new Runnable() {
                                @Override
                                public void run() {
                                    if (irFileErrorCode.errorCode == IrFileErrorCode.IrFileEcOk){
                                        Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_LONG).show();
                                        finish();
                                    }else{
                                        Toast.makeText(MainActivity.this, "保存出错", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
        mIvParams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFragmentByType(FuncType.CommonInfo);
            }
        });
    }

    private void buildItems() {
        FuncType[] values = FuncType.values();
        List<FuncType> itemList = Arrays.asList(values);
        if (mAdapter != null) {
            mAdapter.replaceAll(itemList);
        }
    }

    private void test(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                // storage/emulated/0/IRs/Gallery/Gallery/FOTRIC_20210617_0004.jpg
                File file = new File(Environment.getExternalStorageDirectory() + "/test.txt");
                try {
                    file.createNewFile();
                    FileInputStream fileInputStream = new FileInputStream(file);
                    fileInputStream.read("aasdfa".getBytes());
                    fileInputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initFragments() {
        ThreadManager.getInstance().postToMainHandler(new Runnable() {
            @Override
            public void run() {
                mFuncFragment.clear();
                mFuncFragment.put(FuncType.Image, new ImageFragment(mIRtekFile, MainActivity.this));
                mFuncFragment.put(FuncType.Gain, new GainFragment(mIRtekFile, MainActivity.this));
                mFuncFragment.put(FuncType.ROI, new ROIFragment(mIRtekFile, MainActivity.this));
                mFuncFragment.put(FuncType.Params, new AlarmFragment(mIRtekFile, MainActivity.this));
                mFuncFragment.put(FuncType.palette, new PalatteFragment(mIRtekFile, MainActivity.this));
//                mFuncFragment.put(FuncType.Note, new NoteFragment(mIRtekFile));
                mFuncFragment.put(FuncType.CommonInfo, new CommonFragment(mIRtekFile, MainActivity.this));
                showFragmentByType(FuncType.Image);
            }
        });
    }

    private final Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (mThermalBitmap != null) {
                mIVIrtek.setImageBitmap(mThermalBitmap);
            }
            relativeIRtekView();
        }

        private void relativeIRtekView() {
            if (mHasRelativeViewPosition) {
                addAllMarks();
                return;
            }
            mHasRelativeViewPosition = true;
            mPvIRtek.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mPvIRtek.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (mThermalSize != null) {
                        int height = (int) (mThermalSize.height / (float) mThermalSize.width * mScreenWidth);
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mScreenWidth, height);
                        layoutParams.gravity = Gravity.CENTER;
                        mPvIRtek.setLayoutParams(layoutParams);

                        mRlRois.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                mRlRois.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                addAllMarks();
                            }
                        });
                    }
                }
            });
        }

        private void addAllMarks() {
            mRlRois.removeAllViews();
            IrFileMarkerNames markerNames = mIRtekFile.getMarkerNames();
            for (int i = 0; i < markerNames.count; i++) {
                IrFileMarker marker = mIRtekFile.getMarker(new String(markerNames.names[i], StandardCharsets.UTF_8).trim());
                ShapeType shapeType = getShapeTypeByType(marker.type);
                IrFilePoint[] points = marker.points;
                RectF rectF = new RectF();
                IrFileSize thermalSize = mIRtekFile.getThermalSize();
                if (points.length > 1) {
                    IrFilePoint start = points[0];
                    IrFilePoint end = points[1];
                    rectF.set((float) start.x / thermalSize.width, (float) start.y / thermalSize.height, (float) end.x / thermalSize.width, (float) end.y / thermalSize.height);
                } else if (points.length == 1) {
                    IrFilePoint point = points[0];
                    rectF.set((float) point.x / thermalSize.width, (float) point.y / thermalSize.height, (float) point.x / thermalSize.width, (float) point.y / thermalSize.height);
                }
                ShapeView shaperView = new ShapeView(getBaseContext(), shapeType);
                int parentWidth = mRlRois.getWidth();
                int parentHeight = mRlRois.getHeight();
                RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                rllp.width = rectF.width() > 0 ? (int) (rectF.width() * parentWidth) : 20;
                rllp.height = rectF.height() > 0 ? (int) (rectF.height() * parentHeight) : 20;
                rllp.leftMargin = (int) (rectF.left * parentWidth);
                rllp.topMargin = (int) (rectF.top * parentHeight);
                mRlRois.addView(shaperView, rllp);
            }
        }

        private ShapeType getShapeTypeByType(int type) {
            switch (type) {
                case 1:
                    return ShapeType.Point;
                case 3:
                    return ShapeType.Rect;
                case 4:
                    return ShapeType.Circle;
                case 6:
                    return ShapeType.Line;
            }
            return ShapeType.Point;
        }
    };

    private void showFragmentByType(FuncType funcType) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : mFuncFragment.values()) {
            transaction.hide(fragment);
        }
        Fragment fragment = mFuncFragment.get(funcType);
        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.fragment_container, fragment);
            transaction.show(fragment);
        }
        transaction.commit();
    }

    @Override
    public void refresh() {
        refreshThermalImage();
    }


    @Override
    public void click(float x, float y) {
        try {
            GainFragment gainFragment = (GainFragment) mFuncFragment.get(FuncType.Gain);
            gainFragment.setClickPosition(new PointF(x, y));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}