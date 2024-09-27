plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("C:\\Users\\llpl\\AndroidStudioProjects\\signing\\debugKeystore.jks")
            storePassword = "haris191121"
            keyAlias = "key1"
            keyPassword = "haris191121"
        }
    }
    android.buildFeatures.buildConfig = true
    namespace = "com.cab.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cab.app"
        minSdk = 29
        targetSdk = 35
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
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "web_client_id", System.getenv("GOOGLE_WEB_CLIENT_ID"))
            buildConfigField("String","HERE_API_KEY",System.getenv("HERE_API_KEY1"))
        }
        debug {
            buildConfigField("String","HERE_API_KEY",System.getenv("HERE_API_KEY1"))
        }
    }
    android.applicationVariants.all { variant ->
        val buildTypeName = variant.buildType.name.capitalize()
        // Create a task to modify the file in the assets folder
        tasks.register("replaceVariableInAssets${buildTypeName}") {
            doLast {
                val assetsDir = file("${projectDir}/src/${variant.flavorName ?: "main"}/assets")
                val targetFile = File(assetsDir, "index.js") // Example file in assets

                if (targetFile.exists()) {
                    val content = targetFile.readText()
                    val newContent = content.replace("api", System.getenv("HERE_API_KEY1"))
                    targetFile.writeText(newContent)
                }
            }
        }
        tasks.named("assemble${buildTypeName}").configure {
            dependsOn("replaceVariableInAssets${buildTypeName}")
        }
        return@android
    }

    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.airbnb.android:lottie:6.5.0")
    implementation("androidx.credentials:credentials:1.5.0-alpha04")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-alpha04")
    implementation(project(":material-login"))
    implementation(libs.googleid)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.glide)
    implementation(libs.firebase.storage)
    implementation(libs.okhttp)
    implementation(libs.webkit)
    implementation(libs.play.services.location)
    implementation(libs.firebase.messaging)
    implementation(libs.geofire.android)
    implementation(libs.lifecycle.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}