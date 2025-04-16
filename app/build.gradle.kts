plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.retroplay"
    compileSdk = 35

    viewBinding {
        enable = true
    }

    defaultConfig {
        applicationId = "com.example.retroplay"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true


    }
}

dependencies {

    coreLibraryDesugaring(libs.desugar.jdk.libs)


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.base)
    implementation(libs.navigation.fragment)
    implementation(libs.lifecycle.livedata);
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.storage)
    implementation(libs.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // RecyclerView
    implementation(libs.recyclerview)

    // Glide (una sola versión)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Firebase BoM (Bill of Materials para gestionar versiones automáticamente)
    implementation(platform(libs.firebase.bom))

    // Dependencias de Firebase
    implementation(libs.google.firebase.firestore)
    implementation(libs.google.firebase.auth)

    // Google Sign-In (Fuera de BOM, ya que no es parte de Firebase)
    implementation(libs.play.services.auth)

    // Retrofit para realizar peticiones HTTP
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp para manejo avanzado de solicitudes HTTP
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    implementation (libs.play.services.auth.v2070)
    implementation (libs.com.google.firebase.firebase.auth)

    implementation(libs.play.services.auth.v2100)

}
