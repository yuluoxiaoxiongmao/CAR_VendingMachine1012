package com.example.vendingmachine.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.example.vendingmachine.R;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * 2018/3/23
 */
public class ImageGifView extends RelativeLayout {
    private ImageView imageView;
    private GifImageView gifImageView;
    private GifDrawable headGifDrawable;
    public ImageGifView(Context context) {
        super(context);
        initView(context);
    }

    public ImageGifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context){
        imageView = new ImageView(context);
        LayoutParams layoutParams1 = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(layoutParams1);

        gifImageView = new GifImageView(context);
        LayoutParams layoutParams2 = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams2.addRule(CENTER_IN_PARENT);
        gifImageView.setLayoutParams(layoutParams2);
        addView(imageView);
        addView(gifImageView);

        gifImageView.setImageResource(R.drawable.loading);
        headGifDrawable = (GifDrawable)gifImageView.getDrawable();
        headGifDrawable.setLoopCount(0);
        stop();
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void start(){
        gifImageView.setVisibility(VISIBLE);
        headGifDrawable.start();
    }

    public void stop(){
        gifImageView.setVisibility(GONE);
        headGifDrawable.pause();
    }
}