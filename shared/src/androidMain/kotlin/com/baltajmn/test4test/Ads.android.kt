package com.baltajmn.test4test

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow

// El id de la app de AdMob va en el AndroidManifest de androidApp. En debug y en
// Test Lab (los robots del informe previo al lanzamiento de Play, que lo tocan
// todo) la unidad de prueba de Google: tocar anuncios reales propios es trafico
// no valido. El refresco del banner se configura en la unidad, en la consola.
private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
private const val RELEASE_BANNER = "ca-app-pub-1605179202100483/2077364302"

private fun Context.bannerUnit(): String {
    val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val testLab = Settings.System.getString(contentResolver, "firebase.test.lab") == "true"
    return if (debuggable || testLab) TEST_BANNER else RELEASE_BANNER
}

private object AdMob {
    private val asked = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val running = AtomicBoolean(false)
    val ready = MutableStateFlow(false)
    // El consentimiento ya resuelto no deja pedir anuncios: la franja se recoge.
    val off = MutableStateFlow(false)
    val privacyOptions = MutableStateFlow(false)

    // Una vez por arranque, como pide Google: el formulario si aplica y despues
    // AdMob. Mientras se actualiza, con la respuesta de la vez anterior.
    fun start(activity: Activity) {
        if (!asked.compareAndSet(false, true)) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initialize(activity, consent) } },
            { initialize(activity, consent) },
        )
        initialize(activity, consent, settled = false)
    }

    // Tambien tras cambiarlo en el perfil: retirarlo quita el banner en el acto y
    // darlo despues lo devuelve.
    fun recheck(activity: Activity) = initialize(activity, UserMessagingPlatform.getConsentInformation(activity))

    private fun initialize(context: Context, consent: ConsentInformation, settled: Boolean = true) {
        privacyOptions.value = consent.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds()) {
            ready.value = false
            if (settled) off.value = true
            return
        }
        off.value = false
        if (running.get()) ready.value = true
        if (!started.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G).build()
        )
        val app = context.applicationContext
        // Fuera del hilo principal, como pide Google: tarda.
        thread {
            MobileAds.initialize(app) {
                running.set(true)
                ready.value = consent.canRequestAds()
            }
        }
    }
}

@Composable
actual fun AdBanner(modifier: Modifier) {
    val activity = LocalActivity.current ?: return
    LaunchedEffect(activity) { AdMob.start(activity) }
    val ready by AdMob.ready.collectAsStateWithLifecycle()
    val off by AdMob.off.collectAsStateWithLifecycle()
    // Si no llega anuncio (sin red, sin relleno) la franja se recoge. Un refresco
    // que falla deja el que habia: recogerla moveria las pestanas bajo el dedo.
    var failed by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    if (off || failed) return
    // Franja con su alto desde el primer fotograma: si apareciera al llegar el
    // anuncio, la lista se encogeria y un toque en la ultima fila caeria en el.
    // 12 dp por arriba y por abajo la separan del contenido y de las pestanas.
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val size = AdSize.BANNER
        if (!ready) {
            Box(Modifier.size(size.width.dp, size.height.dp))
            return@Box
        }
        AndroidView(
            modifier = Modifier.size(size.width.dp, size.height.dp),
            factory = {
                AdView(it).apply {
                    setAdSize(size)
                    adUnitId = it.bannerUnit()
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            shown = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (!shown) failed = true
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = AdView::destroy,
        )
    }
}

@Composable
actual fun rememberAdPrivacyOptions(): (() -> Unit)? {
    val required by AdMob.privacyOptions.collectAsStateWithLifecycle()
    val activity = LocalActivity.current ?: return null
    return if (required) ({ UserMessagingPlatform.showPrivacyOptionsForm(activity) { AdMob.recheck(activity) } }) else null
}
