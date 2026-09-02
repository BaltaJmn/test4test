package com.baltajmn.test4test

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val isAndroid: Boolean = true
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun startAppId(): String? = null
