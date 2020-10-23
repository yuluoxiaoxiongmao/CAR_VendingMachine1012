package com.example.vendingmachine.ui

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.vendingmachine.App
import com.example.vendingmachine.utils.widget.CustomToast
import kotlin.properties.Delegates

/**
 * He sun 2018-10-31.
 */
abstract class BaseFragment : Fragment(){

    protected var mActivity : Activity by Delegates.notNull()
    protected var mRootView : ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = this.activity!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (mRootView == null){
            mRootView = inflater.inflate(getBaseLayout(),container,false) as ViewGroup
            initBaseLayout(mRootView)
            onViewAttach()
            initView(savedInstanceState)
        }
        return mRootView
    }

    protected abstract fun getBaseLayout():Int
    protected open fun initBaseLayout(rootView: ViewGroup?){}
    protected open fun onViewAttach(){}
    protected open fun onViewDetach(){}
    protected abstract fun initView(bundle: Bundle?)

    override fun onDestroy() {
        super.onDestroy()
        onViewDetach()
    }


}