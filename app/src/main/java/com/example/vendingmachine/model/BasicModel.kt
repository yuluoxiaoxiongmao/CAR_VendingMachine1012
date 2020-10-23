package com.example.vendingmachine.model

import com.example.vendingmachine.platform.http.Response
import com.example.vendingmachine.platform.http.RxScheduler
import rx.Observable

/**
 * He sun  2018-10-31.
 */
open class BasicModel : BaseModel(){
    fun <T> getNormalRequestData(observable: Observable<T>, response: Response<T>) {
        observable.compose(RxScheduler.applyScheduler<T>()).subscribe(response)
    }
}