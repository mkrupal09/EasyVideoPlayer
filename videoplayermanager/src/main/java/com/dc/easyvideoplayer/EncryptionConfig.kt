package com.dc.easyvideoplayer

import java.io.Serializable

data class EncryptionConfig(val algorithm: String,
                            val key: String) :Serializable
