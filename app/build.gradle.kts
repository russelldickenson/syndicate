plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.application.compose)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

// 1. Read signing inputs from Gradle properties or environment variables
fun signingValue(property: String, envName: String): String? =
    providers.gradleProperty(property)
        .orElse(providers.environmentVariable(envName))
        .orNull

val releaseKeystore = signingValue("signing.storeFile", "ANDROID_KEYSTORE_PATH")
val releaseStorePassword = signingValue("signing.storePassword", "ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("signing.keyAlias", "ANDROID_KEY_ALIAS")
    ?: providers.gradleProperty("signing.defaultKeyAlias").orNull
val releaseKeyPassword = signingValue("signing.keyPassword", "ANDROID_KEY_PASSWORD") ?: releaseStorePassword
val apkOutputFileName = providers.gradleProperty("apk.outputFileName").orNull
    ?: "${rootProject.name}.apk"

android {
    namespace = "com.zigzag.syndicate"
    defaultConfig {
        applicationId = "com.zigzag.syndicate"
        versionCode = 1
        versionName = "1.0.0"
    }

    // Validate signing parameters if a keystore was supplied
    if (releaseKeystore != null) {
        val missing = mutableListOf<String>()
        if (releaseStorePassword == null) missing.add("ANDROID_KEYSTORE_PASSWORD")
        if (releaseKeyAlias == null) missing.add("ANDROID_KEY_ALIAS or signing.defaultKeyAlias")
        if (releaseKeyPassword == null) missing.add("ANDROID_KEY_PASSWORD")
        if (missing.isNotEmpty()) {
            throw GradleException("Incomplete release signing config: ${missing.joinToString(", ")}")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

// Rename the output APK to match apk.outputFileName using the AGP 9 variant API.
// AppExtension / BaseVariantOutputImpl were removed in AGP 9.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(apkOutputFileName)
        }
    }
}

// 2. Prevent CI from producing an unsigned APK silently.
// Validate during configuration: task actions that capture script state break
// Gradle's configuration cache.
if (gradle.startParameter.taskNames.any { it.endsWith("assembleRelease") }) {
    if (System.getenv("CI") == "true" && releaseKeystore == null) {
        throw GradleException(
            "Release signing is not configured in CI. Set ANDROID_KEYSTORE_PATH and ANDROID_KEYSTORE_PASSWORD."
        )
    }
    if (!providers.gradleProperty("apk.outputFileName").isPresent) {
        throw GradleException("Set apk.outputFileName in gradle.properties.")
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
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-navigation3-runtime").get())
    implementation(libs.findLibrary("androidx-navigation3-ui").get())
    implementation(libs.findLibrary("androidx-compose-material3-adaptive-navigation-suite").get())
    implementation(libs.findLibrary("androidx-compose-material3-adaptive").get())
    implementation(libs.findLibrary("androidx-window-core").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    implementation(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
}
