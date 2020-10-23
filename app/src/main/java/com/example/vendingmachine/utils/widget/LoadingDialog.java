package com.example.vendingmachine.utils.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.vendingmachine.App;
import com.example.vendingmachine.R;

/**
 * He sun 2018/10/31.
 */

public class LoadingDialog extends AlertDialog {
    private ImageView iv_load_dialog;
    private AnimationDrawable frameAnim;

    public LoadingDialog(Context context) {
        super(context, R.style.my_dialog_style);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_dialog_layout);
        iv_load_dialog = findViewById(R.id.iv_load_dialog);
        starLouAnim(iv_load_dialog);
    }

    /**
     * 加载动画
     */
    public void starLouAnim(ImageView imageView){
        // 通过逐帧动画的资源文件获得AnimationDrawable示例
        frameAnim=(AnimationDrawable) App.Companion.getMContext().getDrawable(R.drawable.load_anim);
        // 把AnimationDrawable设置为ImageView的背景
        imageView.setBackgroundDrawable(frameAnim);
        start(frameAnim);
    }

    public void start(AnimationDrawable frameAnim) {
        if (frameAnim != null && !frameAnim.isRunning()) {
            frameAnim.start();
        }
    }

}
