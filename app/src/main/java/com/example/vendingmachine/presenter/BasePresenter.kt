package com.example.vendingmachine.presenter

import android.content.Context
import com.example.vendingmachine.model.BaseModel

/**
 * He sun 2018-10-31.
 */
abstract class BasePresenter<T, M : BaseModel> {
    var mView: T? = null//view层
    var mModel: M? = null//Model层
    var mContext: Context? = null

    open fun attachView(context: Context, t: T) {
        if (mModel != null) {
            throw RuntimeException("model出现二次初始化.....")
        }
        if (mView != null) {
            throw RuntimeException("view出现二次初始化.....")
        }
        mView = t
        mContext = context
        initModel()
    }

    open fun detachView() {
        mView = null
        mModel = null
        mContext = null
    }

    abstract fun initModel()
}