plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.material.login"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("com.google.android.material:material:1.12.0")
}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("material-login-page") {
                from(components["release"])
                groupId = "com.github.haris-mehraj"
                artifactId = "material-login-android"
                version = "1.0-beta"

            }
        }
    }
}
