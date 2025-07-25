//Publishing your Kotlin Multiplatform library to Maven Central
//https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-going-public-4a8k

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing
import java.util.*

plugins {
    id("maven-publish")
    id("signing")
}

// Stub secrets to let the project sync and build without the publication values set up
/*ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.secretKeyRingFile"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null*/

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
println("local.properties file exists: ${secretPropsFile.exists()}")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply { load(it) }
    }.onEach { (name, value) ->
        println("local.properties: $name=$value")
        ext[name.toString()] = value
    }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

publishing {
    // Configure maven central repository
    repositories {
        maven("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/") {
            name = "ossrh-staging-api"
            credentials {
                println("ossrhUsername: ${getExtraString("ossrhUsername")}")
                println("ossrhPassword: ${getExtraString("ossrhPassword")}")
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(javadocJar.get())

        // Provide artifacts information requited by Maven Central
        pom {
            name.set("KMP-Player")
            description.set("Kotlin Multiplatform Video Player library")
            url.set("https://github.com/RufenKhokhar/KMP-Player")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    //id.set("") todo
                    name.set("RufenKhokhar")
                    email.set("rufankhokhar@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/RufenKhokhar")
            }
        }
    }
}

// Signing artifacts. Signing.* extra properties values will be used
signing {
    if (getExtraString("signing.keyId") != null) {
        sign(publishing.publications)
    }
}

//https://github.com/gradle/gradle/issues/26132
val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(signingTasks)
}