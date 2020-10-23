package com.example.vendingmachine.utils.widget;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.vendingmachine.R;
import com.example.vendingmachine.platform.common.Constant;

/**
 * HE SUN 2018-11-06.
 */

public class ShipmentsDialog extends AlertDialog {

    private static ShipmentsDialog instance = null;
    private AnimationDrawable frameAnim;
    private CountDownTimer downTimer = null;
    private ImageView iv_shipments_img;
    private ImageView iv_shipments_bj;
    private ImageView iv_shipments_bj_img;
    private boolean Is_Msg = false;
    private ShippingCountdownInterface anInterface;

    public ShipmentsDialog(Context context) {
        super(context, R.style.my_dialog_style);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        frameAnim = (AnimationDrawable) context.getResources().getDrawable(R.drawable.ch_anim);
    }

    public static synchronized ShipmentsDialog getInstance(Context context) {
        if (instance == null)
            instance = new ShipmentsDialog(context);
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipments_dialog_layout);
        iv_shipments_img = findViewById(R.id.iv_shipments_img);
        iv_shipments_bj = findViewById(R.id.iv_shipments_bj);
        iv_shipments_bj_img = findViewById(R.id.iv_shipments_bj_img);

    }

    private void starTimer() {
        if (downTimer == null) {
            downTimer = new CountDownTimer(Constant.CH_TIME, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    handler.sendEmptyMessage(1);
                    Log.e("TAG", "出货倒计时：" + millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    Log.e("TAG", "出货弹窗消失");
                    handler.sendEmptyMessage(2);
                }
            }.start();
        }
    }

    private void cancelTimer() {
        if (downTimer != null) {
            downTimer.cancel();
            downTimer = null;
            Is_Msg = false;
        }
    }

    public void shipmentSuccess(int state) {
        if (instance != null) {
            Is_Msg = true;
            try {
                if (state == 0){
                    iv_shipments_img.setImageResource(R.mipmap.ch_start_img);
                }else if(state == 1){
                    iv_shipments_img.setImageResource(R.mipmap.cz_end);
                }

                frameAnim.stop();
                iv_shipments_bj.setVisibility(View.GONE);
                iv_shipments_bj_img.setVisibility(View.VISIBLE);
                iv_shipments_bj_img.setImageResource(R.mipmap.ch_cg_bj);
                handler.sendEmptyMessageDelayed(3, 2000);
            } catch (Exception e) {
            }
        }
    }

    public void shipmentFail() {
        if (instance != null) {
            Is_Msg = true;
            iv_shipments_img.setImageResource(R.mipmap.ch_fail_img);
            frameAnim.stop();
            iv_shipments_bj.setVisibility(View.GONE);
            iv_shipments_bj_img.setVisibility(View.VISIBLE);
            iv_shipments_bj_img.setImageResource(R.mipmap.ch_sb_bj);
            handler.sendEmptyMessageDelayed(4, 2000);
        }
    }

    private void starLouAnim(ImageView imageView) {
        imageView.setBackgroundDrawable(frameAnim);
        frameAnim.start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (Is_Msg) {
                        cancelTimer();
                    }
                    break;
                case 2:
                    if (anInterface != null) {
                        anInterface.shippingCountdown();
                    }
                    dismissDialog();
                    cancelTimer();
                    break;
                case 3:
                    dismissDialog();
                    break;

                case 4:
                    dismissDialog();
                    break;

            }
        }
    };

    public void showSpDialog(int state) {
        if (instance != null) {
            instance.show();
            if (state == 0){
                //购买出货
                iv_shipments_img.setImageResource(R.mipmap.ch_success_img);
                //消费启动出货等待界面 
                iv_shipments_bj.setVisibility(View.VISIBLE);
                iv_shipments_bj_img.setVisibility(View.GONE);
                starLouAnim(iv_shipments_bj);
                starTimer();
            }else if(state == 1){
                //充值
                //不启动出货等待界面 2020.10.1
                iv_shipments_img.setImageResource(R.mipmap.cz_ing);
            }
            
            //屏蔽2020.10.1
//            iv_shipments_bj.setVisibility(View.VISIBLE);
//            iv_shipments_bj_img.setVisibility(View.GONE);
//            starLouAnim(iv_shipments_bj);
//            starTimer();
        }
    }

    private void dismissDialog() {
        if (instance != null && instance.isShowing()) {
            instance.dismiss();
        }
    }

    public void ShippingCountdownCallback(ShippingCountdownInterface anInterface) {
        this.anInterface = anInterface;
    }

    public interface ShippingCountdownInterface {
        void shippingCountdown();
    }

}
