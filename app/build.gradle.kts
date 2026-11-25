plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.application.compose)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.zigzag.syndicate"
    defaultConfig {
        applicationId = "com.zigzag.syndicate"
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SYNDICATE_KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("SYNDICATE_STORE_PASSWORD") ?: "CHANGE_ME"
            keyAlias = System.getenv("SYNDICATE_KEY_ALIAS") ?: "CHANGE_ME"
            keyPassword = System.getenv("SYNDICATE_KEY_PASSWORD") ?: "CHANGE_ME"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(project(":feature:feeds"))
    implementation(project(":feature:articles"))
    implementation(project(":feature:reader"))
    implementation(project(":feature:settings"))

    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    implementation(libs.findLibrary("androidx-core").get())
    implementation(libs.findBundle("compose").get())
    implementation(libs.findLibrary("androidx-compose-material3").get())
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
    implementation(libs.findLibrary("androidx-compose-material-icons-extended").get())
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-navigation3-runtime").get())
    implementation(libs.findLibrary("androidx-navigation3-ui").get())
    implementation(libs.findLibrary("androidx-compose-material3-adaptive-navigation-suite").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    implementation(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
}
