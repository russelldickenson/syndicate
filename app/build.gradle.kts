plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.application.compose)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.zigzag.rssreader"
    defaultConfig {
        applicationId = "com.zigzag.rssreader"
        versionCode = 1
        versionName = "1.0.0"
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
    implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    implementation(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
}
