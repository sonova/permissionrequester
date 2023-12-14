plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sonova.android.permissionrequester.test"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.test:core-ktx:1.5.0")

    implementation("io.mockk:mockk:1.13.8")

    implementation(project(":permissionrequester"))

}