package com.example.vendingmachine.utils.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.vendingmachine.R;
import com.example.vendingmachine.ui.activity.ReplenishActivity;

/**
 * HE SUN 2018-12-20.
 */

public class FaultDialog extends AlertDialog{

    private EditText et_gz;
    private Button bt_pc_gz;
    private Context context;

    public FaultDialog(Context context) {
        super(context, R.style.my_dialog_style);
        this.context = context;
        setCanceledOnTouchOutside(true);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.failure_dialog_layout);
        et_gz =findViewById(R.id.et_gz);
        bt_pc_gz =findViewById(R.id.bt_pc_gz);

        bt_pc_gz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_gz.getText().toString().equals("357942")){
                    et_gz.setText("");
                    FaultDialog.this.dismiss();
                    yhidekeyboard();
                    context.startActivity(new Intent(context, ReplenishActivity.class));
                }else {
                    CustomToast.getInstance().show(context,"密码错误!");
                }
            }
        });
    }

    private void yhidekeyboard() {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

}
