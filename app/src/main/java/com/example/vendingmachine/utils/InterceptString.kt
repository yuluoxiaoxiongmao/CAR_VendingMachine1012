package com.example.vendingmachine.utils

/**
 * HE 2018-11-01.
 */
class InterceptString{
    companion object {
        fun interceptLetters(str : String): String {
            return str.replace("[^a-zA-Z]".toRegex(), "")
        }

        fun interceptDigital(str : String):String{
            return str.replace("[^0-9]".toRegex(),"")
        }

        fun interceptSpace(str : String):String{
            return str.replace(" ".toRegex(),"")
        }

        fun getNumber(index: Int):String{
            return "请输入A1-A"+index+"货道号"
        }

        fun ImgName(img: String): String {
            val substring = img.substring(img.indexOf("/picture/") + 1, img.indexOf(".png"))
            return substring.replace("/", "")
        }

        fun VideoName(video: String): String {
            val substring = video.substring(video.indexOf("/video/") + 1, video.indexOf(".mp4"))
            return substring.replace("/", "")
        }

        fun getStringIndex(str: String) : String {
            return str.substring(0,str.indexOf("\n"))
        }

        fun moneyByteToByte(str: Int): Byte {
            return str.toByte()
        }

    }






















}