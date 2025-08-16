
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    id("com.vanniktech.maven.publish") version "+"
}

group = "io.github.rufenkhokhar"
version = "1.0.2"

kotlin {
    jvmToolchain(21)

    androidTarget { publishLibraryVariants("release") }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(compose.runtimeSaveable)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
        }
    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }

}

android {
    namespace = "io.github.rufenkhokhar"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }
}
mavenPublishing {

    coordinates(
        groupId = "io.github.rufenkhokhar",
        artifactId = "KMP-Player",
        version = "1.0.2-beta"
    )
    pom {
        name.set("KMP-Player")
        description.set("KMP Video Player")
        inceptionYear.set("2025")
        url.set("https://github.com/rufenkhokhar/KMP-Player")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("RufenKhokhar")
                name.set("RufenKhokhar")
                email.set("Rufankhokhar@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/rufenkhokhar/KMP-Player")
        }
    }
    //publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    publishToMavenCentral()
    signAllPublications()
}