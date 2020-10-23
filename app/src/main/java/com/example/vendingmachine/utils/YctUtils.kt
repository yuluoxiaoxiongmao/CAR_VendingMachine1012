package com.example.vendingmachine.utils

/**
 * HE 2018-11-01.
 */
public class YctUtils {
    //java强转会数据会变  kotlin 就不会。坑爹kh
    fun moneyByteToByte(str: Int): Byte {
        return str.toByte()
    }

}