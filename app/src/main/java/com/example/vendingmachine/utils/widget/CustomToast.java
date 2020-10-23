package com.example.vendingmachine.utils.widget;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vendingmachine.R;
import com.example.vendingmachine.utils.AlertDialogUtils;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 自定义Toast 2018-10-31.
 */

public class CustomToast {

    private static CustomToast customToast;

    private Toast toast;

    public static CustomToast getInstance() {
        if (customToast == null) {
            customToast = new CustomToast();
        }
        return customToast;
    }

    public void show(Context context, String msg) {
        show(context, msg, false);
    }

    public void show(Context context, String massage, boolean success) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_toast_layout, null);
        TextView textView = view.findViewById(R.id.tv_toast_text);
        textView.setText(massage);
        if (toast == null) {
            toast = new Toast(context.getApplicationContext());
            //设置Toast要显示的位置，水平居中并在底部，X轴偏移0个单位，Y轴偏移70个单位，
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 50);
            makeToastSelfViewAnim(R.style.AnimationToast);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(view);
        } else {
            toast.setView(view);
        }
        toast.show();

    }


    private void makeToastSelfViewAnim(int animationID) {
        try {
            Field mTNField = toast.getClass().getDeclaredField("mTN");
            mTNField.setAccessible(true);
            Object mTNObject = mTNField.get(toast);
            Class tnClass = mTNObject.getClass();
            Field paramsField = tnClass.getDeclaredField("mParams");
            /**由于WindowManager.LayoutParams mParams的权限是private*/
            paramsField.setAccessible(true);
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) paramsField.get(mTNObject);
            layoutParams.windowAnimations = animationID;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
