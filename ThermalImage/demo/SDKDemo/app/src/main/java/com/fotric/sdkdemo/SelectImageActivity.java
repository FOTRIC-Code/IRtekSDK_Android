package com.fotric.sdkdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.fotric.irtekfilesdk.IRtekFile;
import com.fotric.irtekfilesdk.IrFileEnumFileType;
import com.fotric.irtekfilesdk.IrFileErrorCode;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;
import com.yuyh.library.imgsel.ISNav;
import com.yuyh.library.imgsel.config.ISListConfig;

import java.util.ArrayList;
import java.util.List;

public class SelectImageActivity extends AppCompatActivity {

    private static final int WRITE_SDCARD_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_LIST_CODE = 1002;
    private final ISListConfig config = new ISListConfig.Builder()
            // 是否多选, 默认true
            .multiSelect(false)
            // 是否记住上次选中记录, 仅当multiSelect为true的时候配置，默认为true
            .rememberSelected(false)
            // “确定”按钮背景色
            .btnBgColor(Color.GRAY)
            // “确定”按钮文字颜色
            .btnTextColor(Color.BLUE)
            // 使用沉浸式状态栏
            .statusBarColor(Color.parseColor("#3F51B5"))
            // 返回图标ResId
            .backResId(R.drawable.ic_close)
            // 标题
            .title("图片")
            // 标题文字颜色
            .titleColor(Color.WHITE)
            // TitleBar背景色
            .titleBgColor(Color.parseColor("#3F51B5"))
            // 裁剪大小。needCrop为true的时候配置
            .cropSize(1, 1, 200, 200)
            .needCrop(false)
            // 第一个是否显示相机，默认true
            .needCamera(false)
            // 最大选择图片数量，默认9
            .maxNum(1)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_image);
    }

    public void selectImage(View view) {
        checkPicturePermission();
    }

    private void checkPicturePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 申请读写内存卡内容的权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_SDCARD_PERMISSION_REQUEST_CODE);
        } else {
            askForPermissions();
        }
    }

    private void select() {
        // 跳转到图片选择器
//        ISNav.getInstance().toListActivity(this, config, REQUEST_LIST_CODE);
        PictureSelector.create(this)
                .openSystemGallery(SelectMimeType.ofImage())
                .setSelectionMode(SelectModeConfig.SINGLE)
                .forSystemResultActivity(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(ArrayList<LocalMedia> result) {
                        if (result != null && result.size() > 0) {
                            String path = result.get(0).getRealPath();
                            IRtekFile file = new IRtekFile(path);
                            IrFileEnumFileType fileType = file.getFileType();
                            if (fileType.errorCode == IrFileErrorCode.IrFileEcOk && fileType.curType != IrFileEnumFileType.IrFileOther) {
                                Intent intent = new Intent(SelectImageActivity.this, MainActivity.class);
                                intent.putExtra(MainActivity.TAG_FILE_PATH, path);
                                startActivity(intent);
                            } else {
                                Toast.makeText(SelectImageActivity.this, "图片不合法", Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    public void askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
        }
        select();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 图片选择结果回调
        if (requestCode == REQUEST_LIST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> pathList = data.getStringArrayListExtra("result");
            if (pathList != null && pathList.size() > 0) {
                String path = pathList.get(0);
                IRtekFile file = new IRtekFile(path);
                IrFileEnumFileType fileType = file.getFileType();
                if (fileType.errorCode == IrFileErrorCode.IrFileEcOk && fileType.curType != IrFileEnumFileType.IrFileOther) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra(MainActivity.TAG_FILE_PATH, path);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "图片不合法", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}