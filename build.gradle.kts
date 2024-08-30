import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    val compileJavaVersion = JavaVersion.VERSION_17
    val targetJavaVersion = JavaVersion.VERSION_17

    pluginManager.withPlugin("java") {
        // javaReleaseVersion can be set to override the global version
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(compileJavaVersion.majorVersion))
            }
        }
        project.tasks.withType<JavaCompile>().configureEach {
            options.release.set(targetJavaVersion.majorVersion.toInt())
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.majorVersion))
                freeCompilerArgs.addAll("-Xjsr305=strict", "-progressive")
                allWarningsAsErrors = true
            }
        }
        if (!project.path.contains("sample") && !project.path.contains("test")) {
            configure<KotlinProjectExtension> { explicitApi() }
        }
    }

    val commonAndroidConfig: CommonExtension<*, *, *, *, *, *>.() -> Unit = {
        compileSdk = 34
        defaultConfig { minSdk = 21 }
        compileOptions {
            sourceCompatibility = compileJavaVersion
            targetCompatibility = targetJavaVersion
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
        lint {
            // https://issuetracker.google.com/issues/243267012
            disable += "Instantiatable"
            checkTestSources = true
        }
        packaging.resources.excludes.add("META-INF/LICENSE*.md")
    }
    // Android library config
    pluginManager.withPlugin("com.android.library") {
        extensions.getByType<LibraryExtension>().apply(commonAndroidConfig)
    }
    pluginManager.withPlugin("com.android.test") {
        extensions.getByType<TestExtension>().apply(commonAndroidConfig)
    }
    // Android app config
    pluginManager.withPlugin("com.android.application") {
        extensions.getByType<ApplicationExtension>().apply(commonAndroidConfig)
    }
}