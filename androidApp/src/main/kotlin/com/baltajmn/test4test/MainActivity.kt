package com.baltajmn.test4test

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // test4test://login: la vuelta del OAuth trae la sesion en el intent.
        supabase.handleDeeplinks(intent)

        setContent {
            App()
        }
    }

    // launchMode singleTop: si la activity ya existe, el redirect llega aqui.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }
}
