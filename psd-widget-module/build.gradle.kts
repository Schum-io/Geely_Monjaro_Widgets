plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.geely.monjaro.psdwidget"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.geely.monjaro.psdwidget"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Xposed API — только для компиляции; реальную реализацию даёт LSposed в рантайме.
    compileOnly("de.robv.android.xposed:api:82")
}
