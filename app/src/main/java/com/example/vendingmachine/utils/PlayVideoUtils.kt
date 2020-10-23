package com.example.vendingmachine.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.vendingmachine.R
import com.example.vendingmachine.platform.common.Constant
import com.example.vendingmachine.ui.customview.MyVideoView
import java.io.File

/**
 * HE 2018-10-31.
 */

class PlayVideoUtils{
    companion object {
        fun loopPlayVideo(context: Context, VideoView : MyVideoView){
            VideoView.setVideoURI(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.voideo_login_bj))
            VideoView.start()
            VideoView.setOnCompletionListener( { VideoView.start() })
        }

        fun starFolderVideo(i: Int,video_view : MyVideoView,videoList : ArrayList<String>) {
            var index : Int = 0
            val video = File(Constant.VIDEO_PATH + "/" + videoList[i] + ".mp4") //得到视频的路径
            video_view.setVideoPath(video.absolutePath) //设置视频(绝对)路径
            video_view.start()
            video_view.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                if (index < videoList.size) {
                    index++
                    starFolderVideo(index,video_view,videoList)
                } else {
                    index = 0
                    starFolderVideo(index,video_view,videoList)
                }
            })
        }

        fun setVolume(volume: Float, `object`: Any) {
            try {
                val forName = Class.forName("android.widget.VideoView")
                val field = forName.getDeclaredField("mMediaPlayer")
                field.isAccessible = true
                val mMediaPlayer = field.get(`object`) as MediaPlayer
                mMediaPlayer.setVolume(volume, volume)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
