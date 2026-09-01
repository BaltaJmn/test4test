package com.baltajmn.test4test

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import test4test.shared.generated.resources.Res
import test4test.shared.generated.resources.app_name
import test4test.shared.generated.resources.google_g
import test4test.shared.generated.resources.login_error
import test4test.shared.generated.resources.login_google
import test4test.shared.generated.resources.login_tagline
import test4test.shared.generated.resources.logo_mark

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val loginErrorText = stringResource(Res.string.login_error)

    PageColumn(modifier) {
        Spacer(Modifier.height(112.dp))
        // La marca antes del nombre: dos checks, el tuyo y el del otro. Es todo lo
        // que hace la app, y cabe en un dibujo.
        Icon(
            painter = painterResource(Res.drawable.logo_mark),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(Res.string.login_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        GoogleSignInButton(
            enabled = !loading,
            onClick = {
                error = null
                loading = true
                scope.launch {
                    // Mismo camino en Android y Web: OAuth por navegador. En
                    // Android vuelve por el deeplink test4test://login.
                    runCatching { supabase.auth.signInWith(Google) }
                        .onFailure { error = it.message ?: loginErrorText }
                    loading = false
                }
            },
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErrorText(error)
    }
}

// Boton oficial de Google Sign-In. Los colores y el alto van fijos, no salen de
// MaterialTheme: las guias de marca de Google fijan el blanco #FFFFFF, el borde
// #747775, el texto #1F1F1F a 14sp medium y una altura de 40dp, y prohiben
// recolorear el logo. Por eso este es el unico sitio de la app con colores a mano.
@Composable
private fun GoogleSignInButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF747775)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White,
            disabledContentColor = Color(0xFF1F1F1F),
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier.fillMaxWidth().height(40.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.google_g),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(Res.string.login_google),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
