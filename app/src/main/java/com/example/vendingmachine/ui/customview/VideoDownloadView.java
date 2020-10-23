package com.example.vendingmachine.ui.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.db.VideoUser;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.platform.http.download.HttpUtils;
import com.example.vendingmachine.utils.InterceptString;
import com.example.vendingmachine.utils.TextLog;
import com.example.vendingmachine.utils.widget.CustomToast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * He 2018-11-15.
 */

public class VideoDownloadView extends FrameLayout implements MediaPlayer.OnErrorListener{

    private Context context;
    public  MyVideoView sp_adv_video;
    public  LinearLayout layout_xz_video;
    private TextView tv_xz_video;
    private ProgressBar pb_xz_video;
    private ArrayList<String> nameList;
    private long mprogress;
    private static int videoSaiz = 0;
    private static int index = 0;
    private static int videoIndex = 0;

    public VideoDownloadView(Context context) {
        super(context,null);
    }

    public VideoDownloadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        nameList = new ArrayList<>();
    }

    private void initView(){
        pb_xz_video = findViewById(R.id.pb_xz_video);
        sp_adv_video = findViewById(R.id.sp_adv_video);
        layout_xz_video = findViewById(R.id.layout_xz_video);
        tv_xz_video = findViewById(R.id.tv_xz_video);
        sp_adv_video.setOnErrorListener(this);

    }

    public void initData(ArrayList<String> urlList, ArrayList<String> nameList){
        HttpUtils.downAddFilevideo(context,urlList,nameList , new HttpUtils.OnDownloadListener() {
            @Override
            public void onDownloadSuccess() {
                handler.sendEmptyMessage(1);
            }

            @Override
            public void onDownloading(long progress) {
                mprogress =  progress;
                handler.sendEmptyMessage(2);
            }

            @Override
            public void onDownloadLength(long max, int size) {
                videoSaiz = size;
                handler.sendEmptyMessage(3);
            }

            @Override
            public void onDownloadFailed() {
                handler.sendEmptyMessage(4);
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    CustomToast.getInstance().show(context,"下载成功");
                    index = 0;
                    App.Companion.getSpUtil().putBoolean(Constant.IS_VIDEO_KEY,true);
                    layout_xz_video.setVisibility(View.GONE);
                    getVideoList();
                    break;
                case 2:
                    pb_xz_video.setProgress((int) mprogress);
                    break;
                case 3:
                    pb_xz_video.setMax(100);
                    index++;
                    String videoText = "正在下载视频广告 "+index+"/"+videoSaiz;
                    tv_xz_video.setText(videoText);
                    break;
                case 4:
                    CustomToast.getInstance().show(context,"广告视频下载失败");
                    TextLog.writeTxtToFile("广告视频下载失败!",Constant.Companion.getFilePath(), Constant.fileName);
                    break;
            }
        }
    };

    public void getVideoList(){
        List<VideoUser> list = App.Companion.getVideoDao().queryForAll();
        nameList.clear();
        for (int i = 0; i < list.size(); i++) {
            nameList.add(InterceptString.Companion.VideoName(list.get(i).getV_url()));
        }
        StarVideo(0);
    }

    public void StarVideo(int i){
        File video = new File(Constant.Companion.getVIDEO_PATH()+"/"+nameList.get(i)+".mp4");
        sp_adv_video.setVideoPath(video.getAbsolutePath());
        sp_adv_video.start();
        sp_adv_video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(videoIndex < nameList.size()-1){
                    videoIndex++;
                    StarVideo(videoIndex);
                }else {
                    videoIndex = 0;
                    StarVideo(videoIndex);
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(context).inflate(R.layout.video_download_layout,this);
        initView();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        layout_xz_video.setVisibility(View.VISIBLE);
        CustomToast.getInstance().show(context,context.getResources().getString(R.string.video_error));
        TextLog.writeTxtToFile("视频播放错误!",Constant.Companion.getFilePath(), Constant.fileName);
        return true;
    }
}
