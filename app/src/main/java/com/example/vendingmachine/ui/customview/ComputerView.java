package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.utils.widget.CustomToast;
/**
 * 2018-09-25.
 */

public class ComputerView extends LinearLayout implements View.OnClickListener{
    private Context context;
    private TextView tv_cargo_lane_id;
    private Button bt_0;
    private Button bt_1;
    private Button bt_2;
    private Button bt_3;
    private Button bt_4;
    private Button bt_5;
    private Button bt_6;
    private Button bt_7;
    private Button bt_8;
    private Button bt_9;
    private Button bt_qc;
    private Button bt_jsj_confirm;
    private StringBuffer sp = null;
    private ButtonInterface buttonInterface;
    public ComputerView(Context context) {
        super(context,null);
    }

    public ComputerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

    }

    public void buttonConfirmOnclick(ButtonInterface anInterface){
        this.buttonInterface = anInterface;
    }

    public void initView(){
        sp = new StringBuffer("A");
        tv_cargo_lane_id = findViewById(R.id.tv_cargo_lane_id);
        bt_jsj_confirm = findViewById(R.id.bt_jsj_confirm);
        bt_0 = findViewById(R.id.bt_0);
        bt_1 = findViewById(R.id.bt_1);
        bt_2 = findViewById(R.id.bt_2);
        bt_3 = findViewById(R.id.bt_3);
        bt_4 = findViewById(R.id.bt_4);
        bt_5 = findViewById(R.id.bt_5);
        bt_6 = findViewById(R.id.bt_6);
        bt_7 = findViewById(R.id.bt_7);
        bt_8 = findViewById(R.id.bt_8);
        bt_9 = findViewById(R.id.bt_9);
        bt_qc = findViewById(R.id.bt_qc);

        bt_jsj_confirm.setOnClickListener(this);
        bt_qc.setOnClickListener(this);
        bt_0.setOnClickListener(this);
        bt_1.setOnClickListener(this);
        bt_2.setOnClickListener(this);
        bt_3.setOnClickListener(this);
        bt_4.setOnClickListener(this);
        bt_5.setOnClickListener(this);
        bt_6.setOnClickListener(this);
        bt_7.setOnClickListener(this);
        bt_8.setOnClickListener(this);
        bt_9.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_0:
                setCargoLaneText("0");
                break;
            case R.id.bt_1:
                setCargoLaneText("1");
                break;
            case R.id.bt_2:
                setCargoLaneText("2");
                break;
            case R.id.bt_3:
                setCargoLaneText("3");
                break;
            case R.id.bt_4:
                setCargoLaneText("4");
                break;
            case R.id.bt_5:
                setCargoLaneText("5");
                break;
            case R.id.bt_6:
                setCargoLaneText("6");
                break;
            case R.id.bt_7:
                setCargoLaneText("7");
                break;
            case R.id.bt_8:
                setCargoLaneText("8");
                break;
            case R.id.bt_9:
                setCargoLaneText("9");
                break;
            case R.id.bt_qc:
                if (sp.length()>0){
                    sp.deleteCharAt(sp.length()-1);
                    tv_cargo_lane_id.setText(sp.toString());
                }
                break;
            case R.id.bt_jsj_confirm:
                if (buttonInterface!=null&&sp.length()>1){
                    buttonInterface.getCargoLaneText(sp.toString());
                }
                break;
        }
    }

    public void setCargoLaneText(String s){
        if (sp.length()<3){
            if (sp.toString().contains("A")) {
                tv_cargo_lane_id.setText(sp.append(s));
            }else {
                sp.append("A");
                tv_cargo_lane_id.setText(sp.append(s));
            }
        }else {
            CustomToast.getInstance().show(App.Companion.getMContext(),"货道号不能大于3位!");
        }
    }

    public interface ButtonInterface {
        void getCargoLaneText(String s);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(context).inflate(R.layout.view_computer_layout,this);
        initView();
    }
}
