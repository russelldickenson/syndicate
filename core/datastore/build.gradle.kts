plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
}

android {
    namespace = "com.zigzag.rssreader.core.datastore"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.findLibrary("androidx-datastore").get())
    implementation(libs.findLibrary("androidx-datastore-preferences").get())
    implementation(libs.findLibrary("kotlinx-serialization").get())

    testImplementation(libs.findBundle("unit-test").get())
}
