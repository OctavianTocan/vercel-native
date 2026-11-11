plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
val release = groovy.json.JsonSlurper().parseText(providers.exec {
    commandLine("python3", "-B", rootProject.file("../deploy/release.py").path, "version")
}.standardOutput.asText.get()) as Map<*, *>
android {
    namespace = "com.octaviantocan.vercelnative"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.octaviantocan.vercelnative"
        minSdk = 32
        targetSdk = 37
        versionCode = (release["versionCode"] as Number).toInt()
        versionName = release["versionName"] as String
        buildConfigField("String", "DOWNLOADS_ORIGIN", "\"https://mymind.octaviantocan.com\"")
        val client = providers.gradleProperty("googleWebClientId").orElse(providers.environmentVariable("ANDROID_GOOGLE_WEB_CLIENT_ID")).getOrElse("")
        require(client.matches(Regex("[A-Za-z0-9._-]*")))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$client\"")
    }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("ANDROID_RELEASE_STORE") ?: "missing-release-key.p12")
            storeType = "PKCS12"
            keyAlias = "vercel-native"
            storePassword = System.getenv("ANDROID_RELEASE_PASSWORD")
            keyPassword = System.getenv("ANDROID_RELEASE_PASSWORD")
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("androidx.fragment:fragment:1.5.7")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    testImplementation("junit:junit:4.13.2")
}
