package com.airiche.sunchip.control

class ViewEventObject {
    val event: Int
    var obj: Any? = null

    constructor(event: Int, obj: Any? = null) {
        this.event = event
        this.obj = obj
    }
}
