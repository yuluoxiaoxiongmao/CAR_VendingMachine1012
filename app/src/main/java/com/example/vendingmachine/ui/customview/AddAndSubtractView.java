package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.vendingmachine.R;
import com.example.vendingmachine.platform.common.Constant;

/**
 * He 2018-11-12.
 */

public class AddAndSubtractView extends LinearLayout implements View.OnClickListener {

    private Context context;
    private Button bt_set_plus;
    private Button bt_set_reduce;
    private TextView tv_set_total;
    private int total;

    public AddAndSubtractView(Context context) {
        super(context, null);
    }

    public AddAndSubtractView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    private void initView() {
        tv_set_total = findViewById(R.id.tv_set_total);
        bt_set_plus = findViewById(R.id.bt_set_plus);
        bt_set_reduce = findViewById(R.id.bt_set_reduce);
        total = Constant.Companion.getSTOCK_TOTAL();
        tv_set_total.setText(total + "");
        bt_set_plus.setOnClickListener(this);
        bt_set_reduce.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_set_plus:
                total++;
                tv_set_total.setText(total + "");
                Constant.Companion.setSTOCK_TOTAL(total);
                break;
            case R.id.bt_set_reduce:
                if (total > 0) {
                    total--;
                    tv_set_total.setText(total + "");
                    Constant.Companion.setSTOCK_TOTAL(total);
                }
                break;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(context).inflate(R.layout.set_number_layout, this);
        initView();
    }

}
