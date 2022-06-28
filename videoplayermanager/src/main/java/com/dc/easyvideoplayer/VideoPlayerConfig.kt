package com.dc.easyvideoplayer

import java.io.Serializable

data class VideoPlayerConfig(
        var videoPath: String? = "",
        var allowPictureInPicture: Boolean = false,
        var autoPlay: Boolean = false,
        var loopVideo: Boolean = false,
        var orientation: Int = ORIENTATION_PORTRAIT_ONLY,
        var startTime: Long = 0L,
        var secureScreen: Boolean = false,
        var drmText: String = "",
        var encryptionConfig: EncryptionConfig? = null
) : Serializable {

    companion object {
        const val ORIENTATION_PORTRAIT_ONLY = 0
        const val ORIENTATION_LANDSCAPE_ONLY = 1
        const val ORIENTATION_USER_ORIENTATION = 2
    }

    class Builder {
        val videoPlayerConfig = VideoPlayerConfig()

        fun videoPath(videoPath: String) = apply { videoPlayerConfig.videoPath = videoPath }
        fun autoPlay(autoPlay: Boolean) = apply { videoPlayerConfig.autoPlay = autoPlay }
        fun loopVideo(loopVideo: Boolean) = apply { videoPlayerConfig.loopVideo = loopVideo }
        fun allowPictureInPicture(allowPictureInPicture: Boolean) =
                apply { videoPlayerConfig.allowPictureInPicture = allowPictureInPicture }

        fun orientation(orientation: Int) =
                apply { videoPlayerConfig.orientation = orientation }


        fun secureScreen() = apply {
            videoPlayerConfig.secureScreen = true
        }

        fun setDrmText(text: String) = apply {
            videoPlayerConfig.drmText = text
        }

        fun setStartTime(time: Long) = apply {
            videoPlayerConfig.startTime = time
        }

        fun encryptionConfigEnable(encryptionConfiguration: EncryptionConfig) = apply {
            videoPlayerConfig.encryptionConfig = encryptionConfiguration
        }

        fun build() = videoPlayerConfig
    }
}