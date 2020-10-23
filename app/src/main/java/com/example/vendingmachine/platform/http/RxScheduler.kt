package com.example.vendingmachine.platform.http

import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


/**
 * Created by jumpbox on 2017/6/15.
 */
class RxScheduler {
    companion object {
        fun <T> applyScheduler(): Observable.Transformer<T, T>? {
            return Observable.Transformer<T, T> { it.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.newThread()) }
        }

    }


}