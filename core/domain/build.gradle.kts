plugins {
    alias(libs.plugins.app.jvm.library)
    alias(libs.plugins.app.kotlin.serialization)
}

dependencies {
    api(libs.findLibrary("kotlinx-coroutines-core").get())
    api(libs.findLibrary("kotlinx-datetime").get())
}
