package com.example.vendingmachine.platform.http;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.KeyEvent;

import com.example.vendingmachine.App;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.utils.LoadingUtils;
import com.example.vendingmachine.utils.TextLog;
import com.example.vendingmachine.utils.widget.CustomToast;
import com.google.gson.JsonSyntaxException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;


public abstract class Response<T> extends Subscriber<T> {

    private Context mContext;
    private boolean mNeedDialog = false;
    private onCancelRequestListener cancelRequestListener;

    public Response(Context context) {
        this.mContext = context;
    }

    public Response() {

    }

    public Response(Context context, boolean needDialog) {
        this.mContext = context;
        this.mNeedDialog = needDialog;
    }

    public Response(Context context, boolean needDialog, onCancelRequestListener cancelRequestListener) {
        this.mContext = context;
        this.mNeedDialog = needDialog;
        this.cancelRequestListener = cancelRequestListener;
    }


    /**
     * 此方法现在onNext或者onError之后都会调用
     * 所以一般要处理请求结束时的信息是，需要重写此方法
     * 例如，loading结束时，刷新下拉刷新结果时等…………
     */
    @Override
    public void onCompleted() {
        if (mNeedDialog) {
            LoadingUtils.dismiss();

        }
        mContext = null;
    }

    @Override
    public void onNext(T str) {
//        if (str instanceof BaseResult) {
//            BaseResult tmp = (BaseResult) str;
//            if (tmp.message.contains("未登录")) {
//                GlobalParam.onExitUser();
//                GlobalParam.isLoginOrJump();
//            }
//        }
        _onNext(str);
    }


    protected abstract void _onNext(T result);

    @Override
    public void onStart() {
        if (mNeedDialog) {
            if (mContext == null) {
                return;
            }
            LoadingUtils.show(mContext);
            LoadingUtils.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                    if (i == KeyEvent.KEYCODE_BACK) {
                        unsubscribe();
                        LoadingUtils.dismiss();
                        if (cancelRequestListener != null) {
                            cancelRequestListener.onCancelRequest();
                        }
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 除非非要获取网络错误信息，否则一般不需要重写此方法；
     * 例如：网络400，404，断网，超时，等等…………
     */
    @Override
    public void onError(Throwable e) {
        onCompleted();
        if (e == null)
            return;
        try {
            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                TextLog.writeTxtToFile("连接服务器失败", Constant.Companion.getFilePath(), Constant.fileName);
                CustomToast.getInstance().show(App.Companion.getMContext(), "连接服务器失败");
            } else if (e instanceof SocketTimeoutException) {
                TextLog.writeTxtToFile("连接超时", Constant.Companion.getFilePath(), Constant.fileName);
                CustomToast.getInstance().show(App.Companion.getMContext(), "连接超时");
            } else if (e instanceof HttpException) {
                TextLog.writeTxtToFile("服务器发生错误", Constant.Companion.getFilePath(), Constant.fileName);
                CustomToast.getInstance().show(App.Companion.getMContext(), "服务器发生错误");
            } else if (e instanceof JsonSyntaxException) {
                TextLog.writeTxtToFile("解析失败", Constant.Companion.getFilePath(), Constant.fileName);
//                CustomToast.getInstance().show(App.Companion.getMContext(), "解析失败");
            } else {

                TextLog.writeTxtToFile("未知错误"+e.toString()+"\r\n"+ e.getLocalizedMessage(), Constant.Companion.getFilePath(), Constant.fileName);
//                CustomToast.getInstance().show(App.Companion.getMContext(), "");
            }
        } catch (Exception ignored) {

        }
        if (e.getMessage() != null) ;
    }

    public interface onCancelRequestListener {
        void onCancelRequest();
    }

}