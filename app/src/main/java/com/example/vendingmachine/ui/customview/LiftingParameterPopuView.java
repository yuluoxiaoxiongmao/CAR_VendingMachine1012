package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.db.ParameterUser;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.serialport.SerialPortUtil;
import com.example.vendingmachine.utils.widget.CustomToast;

import java.util.List;

/**
 * He Sun 2018-11-13.
 */
public class LiftingParameterPopuView extends PopupWindow implements View.OnClickListener{

    private LayoutInflater inflater;
    private View mContentView;
    private Context context;
    private List<ParameterUser> list;
    private int[] height = new int[6];
    private int[] speed = new int[4];
    private int[] savetime = new int[4];
    private EditText et_wz_jb;
    private EditText et_gd_1;
    private EditText et_gd_2;
    private EditText et_gd_3;
    private EditText et_gd_4;
    private EditText et_gd_5;
    private EditText et_gd_6;
    /*private EditText et_gd_7;
    private EditText et_gd_8;
    private EditText et_gd_9;
    private EditText et_gd_10;*/
    private EditText et_fw_sd;
    private EditText et_zg_sd;
    private EditText et_sd_ja;
    private EditText et_sd_jian;
    private EditText et_sj_sz;
    private EditText et_ch_time;
    private EditText et_overtime;
    private EditText et_qh_type;
    private EditText et_qh_cs;

    public LiftingParameterPopuView(Context context) {
        super(context);
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = inflater.inflate(R.layout.popu_parameter_layout,null);
        setContentView(mContentView);
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setOutsideTouchable(false);

        setAnimationStyle(R.style.AnimationShan);
        initView();
        setEtText();
    }

    private void initView(){
        et_wz_jb = mContentView.findViewById(R.id.et_wz_jb);
        et_gd_1 = mContentView.findViewById(R.id.et_gd_1);
        et_gd_2 = mContentView.findViewById(R.id.et_gd_2);
        et_gd_3 = mContentView.findViewById(R.id.et_gd_3);
        et_gd_4 = mContentView.findViewById(R.id.et_gd_4);
        et_gd_5 = mContentView.findViewById(R.id.et_gd_5);
        et_gd_6 = mContentView.findViewById(R.id.et_gd_6);
        /*et_gd_7 = mContentView.findViewById(R.id.et_gd_7);
        et_gd_8 = mContentView.findViewById(R.id.et_gd_8);
        et_gd_9 = mContentView.findViewById(R.id.et_gd_9);
        et_gd_10 = mContentView.findViewById(R.id.et_gd_10);*/
        et_fw_sd = mContentView.findViewById(R.id.et_fw_sd);
        et_zg_sd = mContentView.findViewById(R.id.et_zg_sd);
        et_sd_ja = mContentView.findViewById(R.id.et_sd_ja);
        et_sd_jian = mContentView.findViewById(R.id.et_sd_jian);
        et_sj_sz = mContentView.findViewById(R.id.et_sj_sz);
        et_ch_time = mContentView.findViewById(R.id.et_ch_time);
        et_overtime = mContentView.findViewById(R.id.et_overtime);
        et_qh_type = mContentView.findViewById(R.id.et_qh_type);
        et_qh_cs = mContentView.findViewById(R.id.et_qh_cs);
        mContentView.findViewById(R.id.bt_sj_cs).setOnClickListener(this);
        mContentView.findViewById(R.id.bt_sj_fw).setOnClickListener(this);
        mContentView.findViewById(R.id.bt_bc_height).setOnClickListener(this);
        mContentView.findViewById(R.id.bt_bc_sd).setOnClickListener(this);
        mContentView.findViewById(R.id.bt_fan_hui).setOnClickListener(this);
        mContentView.findViewById(R.id.bt_sj_sz).setOnClickListener(this);
        mContentView.findViewById(R.id.bt_bc_time).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_sj_cs:
                int Y = Integer.parseInt(et_wz_jb.getText().toString());
                SerialPortUtil.getInstance().moto_test(Y);//Y轴步进测试
                break;
            case R.id.bt_sj_fw:
                SerialPortUtil.getInstance().send_moto_reset();//复位
                break;
            case R.id.bt_bc_height:
                try{
                    height[0] = Integer.parseInt(et_gd_1.getText().toString());
                    height[1] = Integer.parseInt(et_gd_2.getText().toString());
                    height[2] = Integer.parseInt(et_gd_3.getText().toString());
                    height[3] = Integer.parseInt(et_gd_4.getText().toString());
                    height[4] = Integer.parseInt(et_gd_5.getText().toString());
                    height[5] = Integer.parseInt(et_gd_6.getText().toString());
                }catch (Exception e){
                    CustomToast.getInstance().show(context,"参数不能为空！");
                    return;
                }
                setParameter(height,1);
                SerialPortUtil.getInstance().send_set_height(height);//保存高度
                break;
            case R.id.bt_bc_sd:
                try {
                    speed[0] = Integer.parseInt(et_zg_sd.getText().toString());
                    speed[1] = Integer.parseInt(et_fw_sd.getText().toString());
                    speed[2] = Integer.parseInt(et_sd_ja.getText().toString());
                    speed[3] = Integer.parseInt(et_sd_jian.getText().toString());
                }catch (Exception e){
                    CustomToast.getInstance().show(context,"参数不能为空！");
                    return;
                }
                setParameter(speed,2);
                SerialPortUtil.getInstance().set_speed(speed);//保存速度
                break;
            case R.id.bt_sj_sz:
                int time = Integer.parseInt(et_sj_sz.getText().toString());
                App.Companion.getSpUtil().putInt(Constant.SET_TIME_KEY,time);
                CustomToast.getInstance().show(context,"设置成功!");
                break;
            case R.id.bt_bc_time:
                try {
                    savetime[0] = Integer.parseInt(et_ch_time.getText().toString());
                    savetime[1] = Integer.parseInt(et_overtime.getText().toString());
                    savetime[2] = Integer.parseInt(et_qh_type.getText().toString());
                    savetime[3] = Integer.parseInt(et_qh_cs.getText().toString());
                }catch (Exception e){
                    CustomToast.getInstance().show(context,"参数不能为空！");
                    return;
                }
                setParameter(savetime,3);
                CustomToast.getInstance().show(context,"保存成功！");
                break;
            case R.id.bt_fan_hui:
                this.dismiss();
                break;
        }
    }

    private void setParameter(int[] cs,int typ){
        list = App.Companion.getSjdao().queryForAll();
        switch (typ){
            case 1:
                App.Companion.getSjdao().update(new ParameterUser(1,list.get(0).getSetting_time(),list.get(0).getY_stepping(),cs[0]+"",
                        cs[1]+"",cs[2]+"",cs[3]+"", cs[4]+"",cs[5]+"", list.get(0).getReset_sd(),list.get(0).getHighest_sd(),list.get(0).getAcceleration(),
                        list.get(0).getDeceleration(),list.get(0).getSptime(),list.get(0).getTimeout(),list.get(0).getTaketype(),list.get(0).getTaketime()));
                break;
            case 2:
                App.Companion.getSjdao().update(new ParameterUser(1,list.get(0).getSetting_time(),list.get(0).getY_stepping()+"",
                        list.get(0).getHeight_1()+"", list.get(0).getHeight_2()+"",list.get(0).getHeight_3()+""
                        ,list.get(0).getHeight_4()+"", list.get(0).getHeight_5()+"",list.get(0).getHeight_6()+"",cs[1]+"",cs[0]+"",
                        cs[2]+"",cs[3]+"",list.get(0).getSptime(),list.get(0).getTimeout(),list.get(0).getTaketype(),list.get(0).getTaketime()));
                break;
            case 3:
                App.Companion.getSjdao().update(new ParameterUser(1,list.get(0).getSetting_time(),list.get(0).getY_stepping()+"",
                        list.get(0).getHeight_1()+"", list.get(0).getHeight_2()+"",list.get(0).getHeight_3()+""
                        ,list.get(0).getHeight_4()+"", list.get(0).getHeight_5()+"",list.get(0).getHeight_6()+"",
                        list.get(0).getReset_sd(),list.get(0).getHighest_sd(),list.get(0).getAcceleration(), list.get(0).getDeceleration(),cs[0]+"",cs[1]+"",cs[2]+"",cs[3]+""));
                break;
        }
    }

    private void setEtText(){
        list = App.Companion.getSjdao().queryForAll();
        et_wz_jb.setText(list.get(0).getY_stepping());
        et_gd_1.setText(list.get(0).getHeight_1());
        et_gd_2.setText(list.get(0).getHeight_2());
        et_gd_3.setText(list.get(0).getHeight_3());
        et_gd_4.setText(list.get(0).getHeight_4());
        et_gd_5.setText(list.get(0).getHeight_5());
        et_gd_6.setText(list.get(0).getHeight_6());
        et_fw_sd.setText(list.get(0).getReset_sd());
        et_zg_sd.setText(list.get(0).getHighest_sd());
        et_sd_ja.setText(list.get(0).getAcceleration());
        et_sd_jian.setText(list.get(0).getDeceleration());
        et_ch_time.setText(list.get(0).getSptime());
        et_overtime.setText(list.get(0).getTimeout());
        et_qh_type.setText(list.get(0).getTaketype());
        et_qh_cs.setText(list.get(0).getTaketime());

        et_sj_sz.setText(App.Companion.getSpUtil().getInt(Constant.SET_TIME_KEY,1)+"");
    }

}
