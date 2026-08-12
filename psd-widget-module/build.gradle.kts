plugins {
    alias(libs.plugins.android.application)
}

val psdWidgetModuleVersionName = "1.1"

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
        versionCode = 2
        versionName = psdWidgetModuleVersionName
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

// Имя выходного APK: psd-widget-module-<version>.apk
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set("psd-widget-module-$psdWidgetModuleVersionName.apk")
        }
    }
}

dependencies {
    // Xposed API — только для компиляции; реальную реализацию даёт LSposed в рантайме.
    compileOnly("de.robv.android.xposed:api:82")
}
