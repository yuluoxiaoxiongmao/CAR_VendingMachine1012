package com.example.vendingmachine.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.example.vendingmachine.App;

/**
 * 网络图片的工具类
 * (1)ImageView设置图片（错误图片）
 * （2）ImageView设置图片---BitMap(不设置默认图)
 * （3）设置RelativeLayout
 * （4）设置LinearLayout
 * （5）设置FrameLayout
 * （6）高斯模糊------ RelativeLayout
 * （7）高斯模糊------ LinearLayout
 * （8）圆角显示图片  ImageView
 */

public class ShowImageUtils {
    /**
     * 显示图片Imageview
     */
    public static void showImageView(Context context, int errorimg, String url,
                                     ImageView imgeview) {
        Glide.with(context).load(url)// 加载图片
                .error(errorimg)// 设置错误图片
                .crossFade()// 设置淡入淡出效果，默认300ms，可以传参
                .placeholder(errorimg)// 设置占位图
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 缓存修改过的图片
                .into(imgeview);
        // Glide.with(context).load(url).thumbnail(0.1f).error(errorimg)
        // .into(imgeview);

        // Glide
        // .with(context)
        // .load(UsageExampleListViewAdapter.eatFoodyImages[0])
        // .placeholder(R.mipmap.ic_launcher) //设置占位图
        // .error(R.mipmap.future_studio_launcher) //设置错误图片
        // .crossFade() //设置淡入淡出效果，默认300ms，可以传参
        // //.dontAnimate() //不显示动画效果
        // .into(imageViewFade);

    }

    /**
     * 获取到Bitmap---不设置错误图片，错误图片不显示
     */
    public static void showImageViewGone(Context context,final ImageView imageView, String url) {
        Glide.with(context).load(url).asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 缓存修改过的图片
                .into(new SimpleTarget<Bitmap>() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> arg1) {
                        imageView.setVisibility(View.VISIBLE);
                        BitmapDrawable bd = new BitmapDrawable(loadedImage);
                        imageView.setImageDrawable(bd);
                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        // TODO Auto-generated method stub
                        super.onLoadFailed(e, errorDrawable);
                        imageView.setVisibility(View.GONE);
                    }

                });
    }

    public static void showLoadingImg(final ImageGifView imageView, ImageView.ScaleType scaleType, String url, int failedImg){
        imageView.getImageView().setScaleType(scaleType);
        if(TextUtils.isEmpty(url)){
            imageView.getImageView().setImageResource(failedImg);
            return;
        }
        imageView.start();
        Glide.with(App.Companion.getMContext().getApplicationContext())
                .load(url)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(failedImg)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<GlideDrawable> target, boolean isFirstResource) {
                        imageView.stop();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
                                                   boolean isFromMemoryCache, boolean isFirstResource) {
                        imageView.stop();
                        return false;
                    }
                })
                .into(imageView.getImageView());
    }


    /**
     * 设置RelativeLayout
     * <p>
     * 获取到Bitmap
     */
    public static void showImageView(Context context, int errorimg, String url, final RelativeLayout bgLayout) {
        Glide.with(context).load(url).asBitmap().error(errorimg)// 设置错误图片
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 缓存修改过的图片
                .placeholder(errorimg)// 设置占位图
                .into(new SimpleTarget<Bitmap>() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> arg1) {
                        BitmapDrawable bd = new BitmapDrawable(loadedImage);
                        bgLayout.setBackground(bd);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        // TODO Auto-generated method stub
                        super.onLoadFailed(e, errorDrawable);
                        bgLayout.setBackgroundDrawable(errorDrawable);
                    }

                });

    }

    /**
     * 设置LinearLayout
     * <p>
     * 获取到Bitmap
     */
    public static void showImageView(Context context, int errorimg, String url, final LinearLayout bgLayout) {
        Glide.with(context).load(url).asBitmap().error(errorimg)// 设置错误图片
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 缓存修改过的图片
                .placeholder(errorimg)// 设置占位图
                .into(new SimpleTarget<Bitmap>() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> arg1) {
                        BitmapDrawable bd = new BitmapDrawable(loadedImage);
                        bgLayout.setBackground(bd);

                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        bgLayout.setBackgroundDrawable(errorDrawable);
                    }
                });

    }

    /**
     * 设置FrameLayout
     * <p>
     * 获取到Bitmap
     */
    public static void showImageView(Context context, int errorimg, String url, final FrameLayout frameBg) {
        Glide.with(context).load(url).asBitmap().error(errorimg)// 设置错误图片
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 缓存修改过的图片
                .placeholder(errorimg)// 设置占位图
                .into(new SimpleTarget<Bitmap>() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> arg1) {
                        BitmapDrawable bd = new BitmapDrawable(loadedImage);
                        frameBg.setBackground(bd);
                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        // TODO Auto-generated method stub
                        super.onLoadFailed(e, errorDrawable);
                        frameBg.setBackgroundDrawable(errorDrawable);
                    }
                });

    }

    /**
     * 获取到Bitmap 高斯模糊         RelativeLayout
     */
    public static void showImageViewBlur(Context context, int errorimg, String url, final RelativeLayout bgLayout) {
        Glide.with(context).load(url).asBitmap().error(errorimg)
                // 设置错误图片
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                // 缓存修改过的图片
                .placeholder(errorimg)
                .transform(new BlurTransformation(context))// 高斯模糊处理
                // 设置占位图
                .into(new SimpleTarget<Bitmap>() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> arg1) {
                        BitmapDrawable bd = new BitmapDrawable(loadedImage);
                        bgLayout.setBackground(bd);
                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        bgLayout.setBackgroundDrawable(errorDrawable);
                    }
                });

    }

    /**
     * 获取到Bitmap 高斯模糊 LinearLayout
     */
    public static void showImageViewBlur(Context context, int url, final LinearLayout bgLayout) {
        Glide.with(context).load(url).asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 设置错误图片

                .transform(new BlurTransformation(context))// 高斯模糊处理
                .into(new SimpleTarget<Bitmap>() {// 设置占位图
                    @SuppressLint("NewApi")
                    @Override
                    public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> arg1) {
                        BitmapDrawable bd = new BitmapDrawable(loadedImage);
                        bgLayout.setBackground(bd);
                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        bgLayout.setBackgroundDrawable(errorDrawable);
                    }
                });

    }

    /**
     * 显示图片 圆角显示  ImageView
     */
    public static void showImageViewToCircle(Application context, int errorimg, String url, ImageView imgeview) {
        Glide.with(context).load(url)
                // 加载图片
                .error(errorimg)
                // 设置错误图片
                .crossFade()
                // 设置淡入淡出效果，默认300ms，可以传参
                .placeholder(errorimg)
                // 设置占位图
                .transform(new GlideCircleTransform(context))//圆角
                .diskCacheStrategy(DiskCacheStrategy.RESULT)// 缓存修改过的图片
                .into(imgeview);
        // Glide.with(context).load(url).thumbnail(0.1f).error(errorimg)
        // .into(imgeview);

        // Glide
        // .with(context)
        // .load(UsageExampleListViewAdapter.eatFoodyImages[0])
        // .placeholder(R.mipmap.ic_launcher) //设置占位图
        // .error(R.mipmap.future_studio_launcher) //设置错误图片
        // .crossFade() //设置淡入淡出效果，默认300ms，可以传参
        // //.dontAnimate() //不显示动画效果
        // .into(imageViewFade);

    }

}
