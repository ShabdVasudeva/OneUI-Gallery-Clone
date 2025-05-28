
plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "apw.sec.android.gallery"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "apw.sec.android.gallery"
        minSdk = 27
        targetSdk = 34
        versionCode = 1620
        versionName = "16.2.0 ApwSpecials"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        
    }
    
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

configurations.all{
    exclude(group = "androidx.preference", module = "preference")
    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.core", module = "core")
    exclude(group = "androidx.drawerlayout", module = "drawerlayout")
    exclude(group = "androidx.viewpager", module = "viewpager")
    exclude(group = "androidx.fragment", module = "fragment")
    exclude(group = "androidx.customview", module = "customview")
    exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
    exclude(group = "com.android.support", module = "support-compat")
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.getstream:photoview:1.0.2")
    implementation("org.mozilla:rhino:1.7.14")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("io.github.oneuiproject:design:1.2.3")
    implementation("io.github.oneuiproject.sesl:indexscroll:1.0.3")
    implementation("io.github.oneuiproject:icons:1.0.1")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("io.github.oneuiproject.sesl:appcompat:1.3.0")
    implementation("io.github.oneuiproject.sesl:material:1.4.0")
    implementation("io.github.oneuiproject.sesl:recyclerview:1.3.0")
    implementation("io.github.oneuiproject.sesl:preference:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
}
