package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.platform.common.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * he sun 2018-11-13.
 */

public class MySpinner extends AppCompatSpinner implements AdapterView.OnItemSelectedListener {

    private List<String> data_list;

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context){

        data_list = new ArrayList<>();
        data_list.add("不带升降、不带掉货检测");
        data_list.add("不带升降、带掉货检测");
        data_list.add("升降弹簧、不带掉货检测");
        data_list.add("升降弹簧、带掉货检测");
        data_list.add("升降履带、不带掉货检测");
        data_list.add("升降履带、带掉货检测");
        data_list.add("升降履带、红外对管");

        ArrayAdapter<String> arrAdapter = new ArrayAdapter<>(context, R.layout.simple_spinner_item,R.id.tv_my_spinner, data_list);
        //设置样式下拉列表框的下拉选项样式
        //arrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //加载适配器
        MySpinner.this.setAdapter(arrAdapter);
        this.setOnItemSelectedListener(this);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position){
            case 0:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,0);
                break;
            case 1:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,1);
                break;
            case 2:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,2);
                break;
            case 3:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,3);
                break;
            case 4:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,4);
                break;
            case 5:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,5);
                break;
            case 6:
                App.Companion.getSpUtil().putInt(Constant.MAC_TYPE_KEY,6);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
