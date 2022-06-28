package com.dc.easyvideoplayer

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class ExoPlayerHelper {
    companion object {
        fun buildMediaSource(context: Context, uri: Uri): MediaSource {
            val mDataSourceFactory =
                DefaultDataSourceFactory(context, Util.getUserAgent(context, "mediaPlayerSample"))
            /*val mDataSourceFactory =
                    CacheDataSourceFactory(VideoCache.get(context), DefaultDataSourceFactory(context,
                            Util.getUserAgent(context, "mediaPlayerSample")))*/
            val factory = when (val type = Util.inferContentType(uri)) {
                C.CONTENT_TYPE_SS -> SsMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                else -> {
                    throw IllegalStateException("Unsupported type: $type") as Throwable
                }
            }
            return factory
        }


        fun buildEncryptedMediaSource(
            context: Context,
            uri: Uri,
            config: EncryptionConfig
        ): MediaSource {
            val dsf = DefaultDataSourceFactory(context, Util.getUserAgent(context, "ExoPlayerInfo"))
            val mediaSource = ProgressiveMediaSource.Factory(
                EncryptedFileDataSourceFactory(dsf.createDataSource(), getCipher(config))
            ).createMediaSource(MediaItem.fromUri(uri))
            return mediaSource
        }


        private fun getCipher(config: EncryptionConfig): Cipher {

            val aes = Cipher.getInstance(config.algorithm);
            aes.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(config.key.toByteArray(Charset.defaultCharset()), "ARC4")
            );
            return aes
        }


        fun playVideo(
            context: Context,
            url: String,
            exoplayerView: CustomExoPlayerView
        ): SimpleExoPlayer {
            val simpleExoPlayer = SimpleExoPlayer.Builder(context).build()
            val mediaSource = buildMediaSource(context, Uri.parse(url))

            simpleExoPlayer.prepare(mediaSource, false, false)
            simpleExoPlayer.playWhenReady = true

            exoplayerView.setShutterBackgroundColor(Color.TRANSPARENT)
            exoplayerView.player = simpleExoPlayer
            return simpleExoPlayer
        }

        fun stopVideo(simpleExoPlayer: SimpleExoPlayer?) {
            simpleExoPlayer?.release()
        }
    }
}

var SimpleExoPlayer.playSpeed: Float
    get() = playbackParameters.speed ?: 1f
    set(speed) {
        val pitch = playbackParameters.pitch ?: 1f
        playbackParameters = PlaybackParameters(speed, pitch)
    }