plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.sonova.android.permissionrequester.test"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.androidx.test.core.ktx)
    implementation(libs.mockk.android)
    implementation(project(":permissionrequester"))
}