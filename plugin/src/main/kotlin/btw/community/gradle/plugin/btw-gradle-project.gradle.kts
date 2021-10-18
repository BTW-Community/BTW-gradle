package btw.community.gradle.plugin

import btw.community.gradle.artifact.mojang
import btw.community.gradle.config.BtwGradleExtension
import btw.community.gradle.util.retrieveExtension
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    application
}

repositories {
    mojang()
    mavenCentral()
}

val config = retrieveExtension<BtwGradleExtension>(project, "btw")

val os = when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
    Os.isFamily(Os.FAMILY_MAC) -> "osx"
    else -> "linux"
}

sourceSets {
    val minecraft by creating {
        java {
            setSrcDirs(listOf(layout.buildDirectory.dir("minecraft/minecraft")))
        }
        resources {
            setSrcDirs(listOf(layout.buildDirectory.dir("minecraft/resources")))
        }
    }

    val minecraftServer by creating {
        java {
            setSrcDirs(listOf(layout.buildDirectory.dir("minecraft/minecraft_server")))
        }
    }

    main {
        java {
            setSrcDirs(listOf(layout.projectDirectory.dir("src/client/java")))
        }
        resources {
            setSrcDirs(listOf(layout.projectDirectory.dir("src/client/resources")))
        }

        compileClasspath += minecraft.compileClasspath + minecraft.output
        runtimeClasspath += minecraft.runtimeClasspath + minecraft.output
    }

    val server by creating {
        compileClasspath += minecraftServer.compileClasspath + minecraftServer.output
        runtimeClasspath += minecraftServer.runtimeClasspath + minecraftServer.output
    }
}

val native: Configuration by configurations.creating
val minecraftImplementation: Configuration = configurations["minecraftImplementation"]
val minecraftCompileClasspath: Configuration = configurations["minecraftCompileClasspath"]

val serverImplementation: Configuration = configurations["serverImplementation"]
val minecraftServerImplementation: Configuration = configurations["minecraftServerImplementation"]
val minecraftServerCompileClasspath: Configuration = configurations["minecraftServerCompileClasspath"]

dependencies {
    minecraftImplementation("mojang:minecraft:${config.modInfo.get().mcversion}:client")
    minecraftServerImplementation("mojang:minecraft:${config.modInfo.get().mcversion}:server")

    minecraftImplementation("org.lwjgl.lwjgl:lwjgl:${config.lwjglVersion}")
    minecraftImplementation("org.lwjgl.lwjgl:lwjgl_util:${config.lwjglVersion}")
    minecraftImplementation("net.java.jinput:jinput:${config.jinputVersion}")

    implementation(sourceSets["minecraft"].output)
    serverImplementation(sourceSets["minecraftServer"].output)

    native("org.lwjgl.lwjgl:lwjgl-platform:${config.lwjglVersion}:natives-$os")
    native("net.java.jinput:jinput-platform:${config.jinputVersion}:natives-$os")
}

application {
    mainClass.set("Start")

    val natives = layout.buildDirectory.dir("natives").get().asFile.absolutePath

    applicationDefaultJvmArgs = listOf("-Xms1024M", "-Xmx1024M", "-Djava.library.path=$natives")

    tasks.run.get().apply {
        val createMinecraftDir by tasks.registering {
            doLast {
                config.minecraftDir.get().asFile.mkdir()
            }
        }
        dependsOn(createMinecraftDir)
        workingDir = config.minecraftDir.get().asFile
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_1_8.majorVersion))
    }

    config.jdkHome.set(
        javaToolchains
            .compilerFor(java.toolchain).get()
            .metadata.installationPath.asFile
    )
}

tasks {
    // TODO Figure out what task to use to export the output addon
    // TODO Make a task that lets you run obfuscated minecraft with your addon

    val natives by tasks.registering(Copy::class) {
        from(native.map { zipTree(it) }) {
            exclude("**/META-INF/**")

            includeEmptyDirs = false
        }

        into(layout.buildDirectory.dir("natives"))
    }

    val copyServerDependencies by registering(Copy::class) {
        dependsOn(minecraftServerCompileClasspath)

        from(minecraftServerCompileClasspath)
        into(layout.buildDirectory.dir("dependencies"))

        exclude("**/*-natives-*.jar")

        rename("([^-]+)-.+", "$1_server.jar")
    }

    val copyClientDependencies by registering(Copy::class) {
        dependsOn(minecraftCompileClasspath)

        from(minecraftCompileClasspath)
        into(layout.buildDirectory.dir("dependencies"))

        exclude("**/*-natives-*.jar")

        rename("([^-]+)-.+", "$1.jar")
    }

    val copyDependencies by registering {
        dependsOn(copyClientDependencies, copyServerDependencies)
    }

    val copyResources by registering(Copy::class) {
        dependsOn(minecraftCompileClasspath)

        includeEmptyDirs = false
        exclude("**/*.class")
        exclude("**/META-INF/**")

        from({
            minecraftCompileClasspath
                .filter { it.name.contains("minecraft") }
                .map { zipTree(it) }
        })
        into(layout.buildDirectory.dir("minecraft/resources"))
    }
    named("processMinecraftResources") { dependsOn(copyResources) }

    distZip { enabled = false }
    distTar { enabled = false }

    val buildMinecraft by registering {
        group = config.taskGroup
        description = "Build Minecraft sources"

        dependsOn(copyDependencies, copyResources)
    }
    compileJava { dependsOn(buildMinecraft) }
    named("compileMinecraftJava") { dependsOn(buildMinecraft) }

    clean {
        group = config.taskGroup
        description = "Clean the project. You will have to recompile Minecraft"
    }

    build {
        group = config.taskGroup
        description = "Build the project"

        dependsOn(natives, buildMinecraft)
    }

    assemble {
        group = config.taskGroup
        description = "Assemble the final mod"
    }
}
