plugins {
    alias(libs.plugins.app.android.feature)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.zigzag.syndicate.feature.reader"
}

dependencies {
    testImplementation(libs.findBundle("unit-test").get())
}
