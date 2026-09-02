package com.baltajmn.test4test

import web.navigator.navigator

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"

    override val isAndroid: Boolean = false
}

actual fun getPlatform(): Platform = JsPlatform()

private fun locationHash(): String = js("window.location.hash") as String

actual fun startAppId(): String? = locationHash().removePrefix("#").takeIf { it.isNotBlank() }
