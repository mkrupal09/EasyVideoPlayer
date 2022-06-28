package com.dc.easyvideoplayer

import android.content.Context
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object VideoCache {
    private var downloadCache: SimpleCache? = null
    private const val maxCacheSize: Long = 20 * 1024 * 1024
    private const val dirName: String = "media"
 
    fun get(context : Context) : SimpleCache{
        if (downloadCache == null) {
            val cacheFolder = File(context.cacheDir, dirName)
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(maxCacheSize)
            downloadCache = SimpleCache(cacheFolder, cacheEvictor)
        }
        return downloadCache as SimpleCache
    }
}