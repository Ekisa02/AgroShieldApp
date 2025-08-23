import org.gradle.kotlin.dsl.implementation





plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.Joseph.agroshieldapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.Joseph.agroshieldapp"
        minSdk = 23
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility =JavaVersion.VERSION_1_8
        targetCompatibility =JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("com.airbnb.android:lottie:6.1.0")
    // Use only the latest version of viewpager2
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta02")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.android.volley:volley:1.2.1")
    // Use only one image loading library (Picasso OR Glide), but both are kept here if you use both in your code.
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    implementation(libs.activity)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation(libs.constraintlayout)
    implementation(libs.google.material)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
   //croping dependencies
    implementation("com.google.mlkit:face-detection:16.1.5")
    // For ExifInterface compatibility
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    // Use only the latest CanHub alternative for image cropping
    // Remove theartofdev and github.CanHub (old/incorrect artifacts)
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    // TFLite
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.3")
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation (platform("com.google.firebase:firebase-bom:31.2.0"))
     // Use only one version of jsoup
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}