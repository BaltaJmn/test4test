import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// Mismo mecanismo que en :shared. El keystore nunca entra en el repo: aqui solo
// viaja la ruta, y local.properties esta gitignored.
val localProperties: Map<String, String> =
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull.orEmpty()
        .lineSequence()
        .filterNot { it.isBlank() || it.trimStart().startsWith("#") }
        .mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 } }
        .associate { (key, value) -> key.trim() to value.trim() }

fun secret(property: String, envVar: String): String =
    providers.environmentVariable(envVar).orNull ?: localProperties[property].orEmpty()

// Si no hay keystore el release sale sin firmar en vez de romper el build: asi la
// CI y cualquiera que clone siguen compilando sin tener la clave de publicacion.
val keystoreFile = secret("keystore.path", "KEYSTORE_PATH")
    .takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }
    ?.takeIf { it.isFile }

android {
    namespace = "com.baltajmn.test4test"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.baltajmn.test4test"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    if (keystoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = secret("keystore.password", "KEYSTORE_PASSWORD")
                keyAlias = secret("keystore.alias", "KEYSTORE_ALIAS")
                keyPassword = secret("keystore.aliasPassword", "KEYSTORE_ALIAS_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
