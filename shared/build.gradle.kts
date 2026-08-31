import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Credenciales de Supabase: variables de entorno en CI, local.properties en local.
// local.properties esta gitignored y el fichero generado vive en build/, asi que
// la anon key nunca entra en un fichero versionado.
val localProperties: Map<String, String> =
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull.orEmpty()
        .lineSequence()
        .filterNot { it.isBlank() || it.trimStart().startsWith("#") }
        .mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 } }
        .associate { (key, value) -> key.trim() to value.trim() }

fun supabaseValue(property: String, envVar: String): Provider<String> =
    providers.environmentVariable(envVar).orElse(localProperties[property] ?: "")

val generateSupabaseConfig by tasks.registering {
    val url = supabaseValue("supabase.url", "SUPABASE_URL")
    val anonKey = supabaseValue("supabase.anonKey", "SUPABASE_ANON_KEY")
    val outputDir = layout.buildDirectory.dir("generated/supabase")

    inputs.property("url", url)
    inputs.property("anonKey", anonKey)
    outputs.dir(outputDir)

    doLast {
        val urlValue = url.get()
        val anonKeyValue = anonKey.get()
        check(urlValue.isNotBlank() && anonKeyValue.isNotBlank()) {
            "Faltan credenciales de Supabase. Copia local.properties.example a local.properties y rellena " +
                "supabase.url y supabase.anonKey, o exporta SUPABASE_URL y SUPABASE_ANON_KEY."
        }
        outputDir.get().file("com/baltajmn/test4test/SupabaseConfig.kt").asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package com.baltajmn.test4test

                // Generado por Gradle. No editar a mano ni versionar: se reescribe en cada build.
                object SupabaseConfig {
                    const val URL: String = "$urlValue"
                    const val ANON_KEY: String = "$anonKeyValue"
                }

                """.trimIndent()
            )
        }
    }
}

kotlin {
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    android {
       namespace = "com.baltajmn.test4test.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain {
            kotlin.srcDir(generateSupabaseConfig)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}