package com.dc.easyvideoplayer

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import androidx.viewpager2.widget.ViewPager2

import com.google.android.exoplayer2.SimpleExoPlayer
import com.dc.easyvideoplayer.databinding.ActivityMediaPagerBinding
import com.dc.easyvideoplayer.databinding.ItemMediaPagerBinding
import easyadapter.dc.com.library.EasyAdapter

class MediaPlayerPagerActivity : AppCompatActivity() {
    lateinit var binding: ActivityMediaPagerBinding
    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private var currentPlayerView: CustomExoPlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_pager)
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()


        val adapter =
            object : EasyAdapter<String, ItemMediaPagerBinding>(R.layout.item_media_pager) {
                override fun onBind(itemBinding: ItemMediaPagerBinding, data: String) {
                    /*currentPlayerView?.player = null
                    currentPlayerView = itemBinding.playerView
                    simpleExoPlayer.playWhenReady = false
                    simpleExoPlayer.stop(true)*/
                    /*simpleExoPlayer.release()*/
                    itemBinding.playerView.player = simpleExoPlayer

                }
            }

        val fragmentAdapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int {
                TODO("Not yet implemented")
            }

            override fun createFragment(position: Int): Fragment {
                TODO("Not yet implemented")
            }

            override fun onViewDetachedFromWindow(holder: FragmentViewHolder) {
                super.onViewDetachedFromWindow(holder)
            }
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        adapter.addOnly("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
        adapter.addOnly("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4")
        adapter.addOnly("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")

        adapter.notifyDataSetChanged()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {


            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                simpleExoPlayer.stop()
                simpleExoPlayer.playWhenReady = false
                val mediaSource = ExoPlayerHelper.buildMediaSource(
                    this@MediaPlayerPagerActivity,
                    Uri.parse(adapter.data[position])
                )
                simpleExoPlayer.prepare(mediaSource, false, false)
                simpleExoPlayer.playWhenReady = true
            }
        })

    }
}