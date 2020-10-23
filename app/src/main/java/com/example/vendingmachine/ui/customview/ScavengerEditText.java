package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

/**
 * HE 2018-09-27.
 */

public class ScavengerEditText extends android.support.v7.widget.AppCompatEditText implements TextWatcher {

    private ScavengingTextInterface textInterface;
    public ScavengerEditText(Context context) {
        super(context);
    }

    public ScavengerEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        addTextChangedListener(this);
    }

    public void ScavengingTextCallBack(ScavengingTextInterface anInterface){
        this.textInterface = anInterface;
    }

    public interface ScavengingTextInterface{
        void getScavengingText(String s);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable s) {//这个方法被调用，那么说明s字符串的某个地方已经被改变。
        String str = s.toString();
        if (str.length() > 2) {
            if (str.endsWith("\n")) {
                if (textInterface != null){
                    textInterface.getScavengingText(str);
                }
                s.clear();
            }
        }
    }
}
