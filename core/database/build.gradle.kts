plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.room)
    alias(libs.plugins.app.hilt)
}

android {
    namespace = "com.zigzag.rssreader.core.database"
}

dependencies {
    implementation(project(":core:domain"))

    testImplementation(libs.findBundle("unit-test").get())
}
