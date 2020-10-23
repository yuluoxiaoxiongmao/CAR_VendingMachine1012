package com.airiche.sunchip.control

import android.util.Log
import org.greenrobot.eventbus.EventBus
import java.util.*


object ViewEventNotifier {
    private val TAG = "ViewEventNotifier"

    fun addEventListener(subscriber: Any) {
        synchronized(ViewEventNotifier::class) {
            Log.e(TAG, "addEventListener: " + subscriber.toString())
            EventBus.getDefault().register(subscriber)
        }
    }

    fun removeEventListener(subscriber: Any) {
        synchronized(ViewEventNotifier::class) {
            Log.e(TAG, "removeEventListener: " + subscriber.toString())
            EventBus.getDefault().unregister(subscriber)
        }
    }


    fun sendMessage(event: Int, obj: Any? = null) {
        synchronized(ViewEventNotifier::class) {
            EventBus.getDefault().post(ViewEventObject(event, obj))
        }
    }
}
