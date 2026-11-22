plugins {
    alias(libs.plugins.app.jvm.library)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.findBundle("unit-test").get())
}
