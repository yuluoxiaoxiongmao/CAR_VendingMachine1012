package com.example.vendingmachine.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import com.example.vendingmachine.App
import com.example.vendingmachine.R
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.presenter.LoginPresenter
import com.example.vendingmachine.ui.BaseActivity
import com.example.vendingmachine.utils.PlayVideoUtils
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : BaseActivity(),LoginPresenter.ILoginView,LoginPresenter.IGoodsInfoView {

    private var number : String? = null
    private var mPresenter : LoginPresenter<LoginPresenter.ILoginView>? = null
    //6.0之后需要动态添加权限
    private val mPermission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_PHONE_STATE)

    override fun getBaseLayout(): Int = R.layout.activity_login

    override fun onViewAttach() {
        super.onViewAttach()
        mPresenter = LoginPresenter()
        mPresenter?.attachView(this,this)
    }

    override fun onViewDetach() {
        super.onViewDetach()
        mPresenter?.detachView()
        mPresenter = null
//        my_video_view.stopPlayback()
    }

    override fun initView(bundle: Bundle?) {
//        PlayVideoUtils.loopPlayVideo(this,my_video_view)
        iv_log.visibility = View.GONE
        if (App.spUtil?.getBoolean(Constant.IS_LOGIN_KEY,false)==true) {
            Handler().postDelayed({
                startActivity(Intent(this, DrinkMacActivity::class.java))
                //startActivity(Intent(this, TestVideoPlayback::class.java))
                finish()
            }, 2000)
        } else {
            iv_log.visibility = View.GONE
            layout_dl_macid.visibility = View.VISIBLE
            setPermissions()
        }
        bt_confirm.setOnClickListener {
            number = et_number.text.toString()
            val password = et_number_pass.text.toString()
            mPresenter?.login(Constant.CODE_ZERO, number!!,password)
            bt_confirm.isEnabled = false
        }
    }

    override fun loginSuccess() {
        toastShow(resources.getString(R.string.login_success))
        mPresenter?.getGoodsInfo(Constant.CODE_ONE, number.toString())
    }

    override fun loginFail(errMsg: String) {
        toastShow(errMsg)
        bt_confirm.isEnabled = true
    }

    private fun setPermissions(){
        val permissionList = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for (i in mPermission.indices){
                if (ContextCompat.checkSelfPermission(this.applicationContext,mPermission[i]) != PackageManager.PERMISSION_GRANTED){
                    permissionList.add(mPermission[i])
                }
            }
            if (!permissionList.isEmpty()){
                val permission = permissionList.toArray(arrayOfNulls<String>(permissionList.size))
                ActivityCompat.requestPermissions(this,permission,1)
            }
        }
    }

    override fun goodsInfoSuccess() {
        //toastShow(resources.getString(R.string.init_good  s))
        startActivity(Intent(this,DrinkMacActivity::class.java))
        //startActivity(Intent(this,TestVideoPlayback::class.java))
        finish()
    }

    override fun goodsInfoFail(errMsg: String) {
        toastShow(errMsg)
    }

    override fun onStop() {
        super.onStop()
//        my_video_view.pause()
    }

    override fun onResume() {
        super.onResume()
//        my_video_view.start()
    }

}
