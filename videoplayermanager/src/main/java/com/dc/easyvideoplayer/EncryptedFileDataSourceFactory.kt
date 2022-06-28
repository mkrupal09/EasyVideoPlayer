package com.dc.easyvideoplayer

import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import javax.crypto.Cipher

class EncryptedFileDataSourceFactory(var dataSource: DataSource, var cipher: Cipher) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return EncryptedDataSource(cipher, object : TransferListener {
            override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

            }

            override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

            }

            override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {

            }

            override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

            }
        })
    }
}