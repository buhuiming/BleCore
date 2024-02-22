package com.bhm.support.sdk.entity

import java.io.Serializable

/**
 * @author Buhuiming
 */
class MessageEvent : Serializable {
    var msgId = 0
    var msg: String? = null
    var data: Any? = null
}