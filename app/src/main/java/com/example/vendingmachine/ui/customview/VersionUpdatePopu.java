package com.example.vendingmachine.ui.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import com.example.vendingmachine.R;
import com.example.vendingmachine.platform.http.download.HttpUtils;
import com.example.vendingmachine.utils.widget.CustomToast;

import java.io.File;

/**
 * HE 2018-11-14.
 */

public class VersionUpdatePopu extends PopupWindow{

    private LayoutInflater inflater;
    private View mContentView;
    private Context context;
    private String apkUrl;
    private Button bt_vi_confirm;
    private ProgressBar pb_version_updata;
    private long apkprogress = 0;

    public VersionUpdatePopu(Context context,String apkUrl) {
        super(context);
        this.context = context;
        this.apkUrl = apkUrl;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = inflater.inflate(R.layout.popu_version_update_layout,null);
        setContentView(mContentView);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setAnimationStyle(R.style.AnimationFade);
        setBackgroundDrawable(new ColorDrawable());
        initView();

    }

    private void initView(){
        bt_vi_confirm = mContentView.findViewById(R.id.bt_vi_confirm);
        pb_version_updata = mContentView.findViewById(R.id.pb_version_updata);

        bt_vi_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bt_vi_confirm.setVisibility(View.GONE);
                pb_version_updata.setVisibility(View.VISIBLE);
                Download_APK(apkUrl,"VendingMachine");
            }
        });
    }

    private void Download_APK(String apkurl,String filename){
        HttpUtils.downFile(apkurl, filename, new HttpUtils.OnDownloadListener() {
            @Override
            public void onDownloadSuccess() {
                handler.sendEmptyMessage(3);
            }

            @Override
            public void onDownloading(long progress) {
                apkprogress = progress;
                handler.sendEmptyMessage(2);
            }
            @Override
            public void onDownloadLength(long max, int size) {
                handler.sendEmptyMessage(4);
            }

            @Override
            public void onDownloadFailed() {
                handler.sendEmptyMessage(5);
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 2:
                    pb_version_updata.setProgress((int)apkprogress);
                    break;
                case 3:
                    CustomToast.getInstance().show(context,"下载成功，请安装新版本!");
                    VersionUpdatePopu.this.dismiss();
                    updateApk();
                    break;
                case 4:
                    pb_version_updata.setMax(100);
                    break;
                case 5:
                    CustomToast.getInstance().show(context,"下载失败!");
                    VersionUpdatePopu.this.dismiss();
                    break;
            }
        }
    };

    /**
     * apk下载完成后启动安装
     */
    private void updateApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "VendingMachine"+".apk")), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
