package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.example.vendingmachine.R;
import com.example.vendingmachine.utils.widget.CustomToast;

/**
 * HE 2018-11-19.
 */

public class PasswordPopu extends PopupWindow{

    private View view;
    private PasswordInterface inflate;
    private EditText et_password_popu;
    private Context context;

    public PasswordPopu(final Context context) {
        super(context);
        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.password_popu_layout,null);
        setContentView(view);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(0x00000000));
        //setOutsideTouchable(false);
        setAnimationStyle(R.style.AnimationFade);

        et_password_popu = view.findViewById(R.id.et_password_popu);
        yhidekeyboard();

        view.findViewById(R.id.bt_password_popu_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = et_password_popu.getText().toString();
                if(s.equals("357942")){//156908790kctlyh
                    if (inflate != null){
                        PasswordPopu.this.dismiss();
                        yhidekeyboard();
                        et_password_popu.setText("");
                        inflate.correctPassword();
                    }
                }else {
                    CustomToast.getInstance().show(context,"密码错误!");
                }
            }
        });
    }

    public void correctPasswordCallback(PasswordInterface inflate) {
        this.inflate = inflate;
    }

    public interface PasswordInterface{
        void correctPassword();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        yhidekeyboard();
    }

    private void yhidekeyboard(){
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

}
