plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
}

android {
    namespace = "com.zigzag.rssreader.core.network"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.findLibrary("okhttp3").get())
    implementation(libs.findLibrary("okhttp3-logging-interceptor").get())
    implementation(libs.findLibrary("kotlinx-datetime").get())
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())

    testImplementation(libs.findLibrary("mockwebserver").get())
    testImplementation(libs.findBundle("unit-test").get())
}
