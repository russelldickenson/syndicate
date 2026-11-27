plugins {
    alias(libs.plugins.app.android.feature)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.zigzag.syndicate.feature.articles"
}

dependencies {
    androidTestImplementation(libs.findLibrary("androidx-compose-ui-test-junit4").get())
    androidTestImplementation(libs.findLibrary("androidx-compose-ui-test-manifest").get())
    androidTestImplementation(libs.findLibrary("androidx-espresso-accessibility").get())
}
