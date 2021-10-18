import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.5.0"
}

tasks.withType<KotlinCompile> {
    val javaVersion = JavaVersion.VERSION_1_8.toString()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    kotlinOptions {
        jvmTarget = javaVersion
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.cloudbees:diff4j:1.3")
    implementation("io.sigpipe:jbsdiff:1.0")
}

fun property(name: String) = project.property(name) as String

gradlePlugin {
    plugins {
        create(property("id")) {
            id = property("id")
            displayName = property("name")
            implementationClass = property("implementation-class")
            version = property("version")
            description = property("description")
        }
    }
}

publishing {
    repositories {
        maven {
            val mainProject = gradle.parent?.rootProject ?: project
            name = "BuildRepository"
            url = uri(mainProject.layout.buildDirectory.dir("repository"))
        }
    }

    publications {
        create<MavenPublication>(name) {
            groupId = property("group")
            artifactId = property("name")
            version = property("version")

            from(components["java"])

            pom {
                name.set(property("name"))
                description.set(property("description"))
                url.set(property("url"))

                developers {
                    developer {
                        name.set("Simon Meskens")
                        url.set("https://github.com/SimonMeskens")
                        organization.set("BTW-Community")
                        organizationUrl.set("https://github.com/BTW-Community")
                    }
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://tldrlegal.com/license/mit-license")
                    }
                }
            }
        }
    }
}
