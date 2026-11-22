plugins {
    alias(libs.plugins.app.jvm.library)
}

dependencies {
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
    implementation(libs.findLibrary("java-inject").get())
}
