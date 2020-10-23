package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.db.GoodsUser;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.platform.http.HttpUrl;
import com.example.vendingmachine.utils.ImageGifView;
import com.example.vendingmachine.utils.ShowImageUtils;

import java.util.List;

/**
 * 2018-11-02.
 */

public class PurchasePopuView extends PopupWindow implements View.OnClickListener {

    private Context constant;
    private List<GoodsUser> list;
    private LayoutInflater inflater;
    private View mContentView;

    public TextView tv_buy_countdown;
    private TextView tv_pay_name;
    private TextView tv_pay_price;
    public ImageView iv_pay_code;
    private ImageGifView iv_pay_goods_img;
    private CheckBox cb_wechat;
    private CheckBox cb_alipay;
    private Button bt_buy_return;
    private TextView tv_pay_box;
    private ImageView img_buy_return;
    private CheckBox cb_yct;
    private TextView tv_pay_way_tip;//选择支付方式提示

    private PayPopuInterface anInterface;

    public PurchasePopuView(Context context, int index, int state, int code) {
        super(context);
        this.constant = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = inflater.inflate(R.layout.buy_popu_layout, null);
        setContentView(mContentView);
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        setAnimationStyle(R.style.AnimationFade);
        setBackgroundDrawable(new ColorDrawable());
        initView(index, state, code);
    }

    private void initView(int index, int state, int code) {
        list = App.Companion.getSpDao().queryForAll();

        tv_buy_countdown = mContentView.findViewById(R.id.tv_buy_countdown);
        iv_pay_code = mContentView.findViewById(R.id.iv_pay_code);
        cb_wechat = mContentView.findViewById(R.id.cb_wechat);
        cb_alipay = mContentView.findViewById(R.id.cb_alipay);
        iv_pay_goods_img = mContentView.findViewById(R.id.iv_pay_goods_img);
        tv_pay_name = mContentView.findViewById(R.id.tv_pay_name);
        tv_pay_price = mContentView.findViewById(R.id.tv_pay_price);
        bt_buy_return = mContentView.findViewById(R.id.bt_buy_return);
        tv_pay_box = mContentView.findViewById(R.id.tv_pay_box);
        tv_pay_way_tip = mContentView.findViewById(R.id.tv_pay_way_tip);
        bt_buy_return.setOnClickListener(this);
        cb_alipay.setOnClickListener(this);
        cb_wechat.setOnClickListener(this);

        img_buy_return = mContentView.findViewById(R.id.img_buy_return);
        img_buy_return.setOnClickListener(this);
        SelectPayment(cb_wechat);

        cb_yct = mContentView.findViewById(R.id.cb_yct);
        cb_yct.setOnClickListener(this);

        tv_pay_name.setText(list.get(index).getName());
        tv_pay_price.setText("￥ ： " + list.get(index).getPrice());
        tv_pay_box.setText("位置号码 ： " + (index + 1));

        ShowImageUtils.showLoadingImg(iv_pay_goods_img, ImageView.ScaleType.CENTER_CROP,
                list.get(index).getPic_url(), R.drawable.loading);
        if (state == 1) {
            //二维码购买
            tv_pay_way_tip.setText("请选择支付方式");
            if (code == 0) cb_alipay.setBackgroundResource(R.drawable.bt_alipay_bj);
            else cb_alipay.setBackgroundResource(R.drawable.yct_icon);
        } else {
            //羊城通购买
            tv_pay_way_tip.setText("请将羊城通放置下方卡槽");
            cb_alipay.setVisibility(View.GONE);
            cb_wechat.setVisibility(View.GONE);
            cb_yct.setVisibility(View.VISIBLE);
            cb_alipay.setBackgroundResource(R.drawable.bt_alipay_bj);
            Glide.with(mContentView.getContext()).load(R.mipmap.card_tip_h).asGif().diskCacheStrategy(DiskCacheStrategy.SOURCE).into(iv_pay_code);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_buy_return:
                if (anInterface != null) {
                    anInterface.OnPopuClickInterface(0);
                }
                break;

            case R.id.cb_alipay:
                Constant.Companion.setIS_QUERY(false);
                Constant.Companion.setPAY_ZF("ali");
                SelectPayment(cb_alipay);
                if (anInterface != null) {
                    anInterface.OnPopuClickInterface(1);
                }
                break;

            case R.id.cb_wechat:
                Constant.Companion.setIS_QUERY(false);
                Constant.Companion.setPAY_ZF("wx");
                SelectPayment(cb_wechat);
                if (anInterface != null) {
                    anInterface.OnPopuClickInterface(2);
                }
                break;
            case R.id.cb_yct:
                Constant.Companion.setIS_QUERY(false);
                Constant.Companion.setPAY_ZF("yct");
                SelectPayment(cb_wechat);
                if (anInterface != null) {
                    anInterface.OnPopuClickInterface(3);
                }
                break;

            case R.id.img_buy_return:
                if (anInterface != null) {
                    anInterface.OnPopuClickInterface(0);
                }
                break;
        }
    }

    private void SelectPayment(CheckBox checkBox) {
        cb_alipay.setChecked(false);
        cb_wechat.setChecked(false);
        checkBox.setChecked(true);
    }

    public interface PayPopuInterface {
        void OnPopuClickInterface(int id);
    }

    public void OnPopuItmOnclick(PayPopuInterface anInterface) {
        this.anInterface = anInterface;
    }

}
