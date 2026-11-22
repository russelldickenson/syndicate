plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.library.compose)
    alias(libs.plugins.app.hilt)
}

android {
    namespace = "com.zigzag.rssreader.core.ui"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.findBundle("compose").get())
    implementation(libs.findLibrary("androidx-compose-ui-graphics").get())
    implementation(libs.findLibrary("androidx-compose-material3").get())
    implementation(libs.findLibrary("coil-compose").get())
    implementation(libs.findLibrary("coil-network-okhttp").get())

    testImplementation(libs.findBundle("unit-test").get())
}
