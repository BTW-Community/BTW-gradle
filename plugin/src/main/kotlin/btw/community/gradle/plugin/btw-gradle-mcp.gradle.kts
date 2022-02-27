package btw.community.gradle.plugin

import btw.community.gradle.artifact.btwGradle
import btw.community.gradle.artifact.mojang
import btw.community.gradle.config.BtwGradleExtension
import btw.community.gradle.patch.task.PatchAction
import btw.community.gradle.util.retrieveExtension
import org.apache.tools.ant.taskdefs.condition.Os

val config = retrieveExtension<BtwGradleExtension>(project, "btw")

repositories {
    btwGradle()
    mojang()
    mavenCentral()
}

val mcpArchive: Configuration by configurations.creating
val btwSource: Configuration by configurations.creating

dependencies {
    mcpArchive("de.oceanlabs.mcp:mcp:751@zip")

    val btwVersion = config.modInfo.get().btwversion

    if (btwVersion.isNotEmpty()) {
        btwSource("btw.community:btw:$btwVersion@zip")
    }
}

tasks {
    val copyDependencies by existing
    val natives by existing

    val prepareMCP by registering(Copy::class) {
        dependsOn(mcpArchive, copyDependencies, natives)

        onlyIf {
            !config.mcpDir.file("conf/mcp.cfg").get().asFile.exists()
        }

        doFirst {
            println("Preparing MCP...")
        }

        from(mcpArchive.map { zipTree(it) }) {
            include("**/conf/**")
            include("**/runtime/**")

            includeEmptyDirs = false
        }
        into(config.mcpDir)

        val commands = config.mcpDir.file("runtime/commands.py")
        val mcpConfig = config.mcpDir.file("conf/mcp.cfg")

        doLast {
            val commandsFile = commands.get().asFile

            val lines = commandsFile.readLines()

            commandsFile.printWriter().use { out ->
                var linenumber = 1
                lines.forEach {
                    @Suppress("MagicNumber")
                    if (linenumber in (704..752)) {
                        val jdkBin = "${config.jdkHome.get()}${File.separator}${"bin"}".replace("\\", "\\\\")
                        if (linenumber == 704) {
                            out.println("        results = [os.path.normpath('$jdkBin')]")
                        }
                    } else out.println(it)

                    linenumber += 1
                }
            }

            val mcpConfigFile = mcpConfig.get().asFile

            fun relativeToMCP(provider: Provider<Directory>) =
                provider.get().asFile.relativeTo(config.mcpDir.get().asFile)

            val text = mcpConfigFile.readText()
                .replace(
                    "DirSrc     = src",
                    "DirSrc     = ${relativeToMCP(layout.buildDirectory.dir("minecraft"))}"
                )
                .replace(
                    "DirJars    = jars",
                    "DirJars    = ${relativeToMCP(layout.buildDirectory.dir("dependencies"))}"
                )
                .replace(
                    "DirReobf   = reobf",
                    "DirReobf   = ${relativeToMCP(layout.buildDirectory.dir("reobf"))}"
                )
                .replace(
                    "DirNatives = %(DirJars)s/bin/natives",
                    "DirNatives = ${relativeToMCP(layout.buildDirectory.dir("natives"))}"
                )
                .replace("%(DirJars)s/bin/", "%(DirJars)s/")

            mcpConfigFile.writeText(text)
        }
    }

    fun command(name: String): MutableList<String> =
        if (Os.isFamily(Os.FAMILY_WINDOWS)) mutableListOf(
            "cmd", "/c", "runtime\\bin\\python\\python_mcp", "runtime/$name.py"
        )
        else mutableListOf("python", "runtime/$name.py")

    val preparePatches by registering(Sync::class) {
        eachFile {
            path = "^.+patches\\/(.+)$".toRegex().find(path)?.groups?.get(1)?.value ?: path
        }

        from(btwSource.map { zipTree(it) })
        into(layout.buildDirectory.dir("patches"))

        include("**/patches/**")
        exclude("**/README.md")

        includeEmptyDirs = false
    }

    val prepareResources by registering(Copy::class) {
        dependsOn(named("processMinecraftResources"))

        eachFile {
            path = "^.+resources\\/(.+)$".toRegex().find(path)?.groups?.get(1)?.value ?: path
        }

        from(btwSource.map { zipTree(it) })
        into(layout.buildDirectory.dir("minecraft/resources"))

        include("**/resources/**")

        includeEmptyDirs = false
    }

    val decompile by registering(Exec::class) {
        dependsOn(prepareMCP, preparePatches, prepareResources)

        onlyIf {
            layout.buildDirectory.dir("minecraft/minecraft").get().asFileTree.isEmpty
        }

        doFirst {
            println("Generating Minecraft source...")
        }

        outputs.dir(layout.buildDirectory.dir("minecraft/minecraft"))
        if (config.serverEnabled.get()) {
            outputs.dir(layout.buildDirectory.dir("minecraft/minecraft_server"))
        }

        workingDir(config.mcpDir.get().asFile)
        commandLine(
            command("decompile").apply {
                if (!config.serverEnabled.get()) {
                    add("--client")
                }
                add("--norecompile")
            }
        )

        doLast {
            if (!layout.buildDirectory
                .file("minecraft/minecraft/net/minecraft/src/FCBetterThanWolves.java")
                .get().asFile.exists()
            ) {
                val target = layout.buildDirectory.dir("minecraft").get().asFile
                val patches = layout.buildDirectory.dir("patches").get().asFileTree.files

                PatchAction(target, patches).execute()
            }
        }
    }

    val updatemd5 by registering(Exec::class) {
        dependsOn(decompile)

        outputs.file(layout.buildDirectory.file("mcp/temp/client.md5"))
        outputs.file(layout.buildDirectory.file("mcp/temp/server.md5"))

        workingDir(config.mcpDir.get().asFile)
        commandLine(
            command("updatemd5").apply {
                if (!config.serverEnabled.get()) {
                    add("--client")
                }
                add("--force")
            }
        )
    }
    named("buildMinecraft") { dependsOn(updatemd5) }

    val recompile by registering(Exec::class) {
        dependsOn(decompile)

        inputs.dir(layout.projectDirectory.dir("src/client"))
        inputs.dir(layout.projectDirectory.dir("src/server"))

        outputs.dir(layout.buildDirectory.dir("mcp/bin"))

        workingDir(config.mcpDir.get().asFile)
        commandLine(
            command("recompile").apply {
                if (!config.serverEnabled.get()) {
                    add("--client")
                }
            }
        )

        doFirst {
            sync {
                from(layout.buildDirectory.dir("minecraft/minecraft")) {
                    into("minecraft")
                }
                from(layout.buildDirectory.dir("minecraft/minecraft_server")) {
                    into("minecraft_server")
                }
                into(layout.buildDirectory.dir("minecraft/temp"))

                exclude("**/resources/**")
            }

            exec {
                workingDir(config.mcpDir.get().asFile)
                commandLine(
                    command("updatemd5").apply {
                        if (!config.serverEnabled.get()) {
                            add("--client")
                        }
                        add("--force")
                    }
                )
            }

            copy {
                from(layout.projectDirectory.dir("src/client/java"))
                into(layout.buildDirectory.dir("minecraft/minecraft"))
            }

            copy {
                from(layout.projectDirectory.dir("src/server/java"))
                into(layout.buildDirectory.dir("minecraft/minecraft_server"))
            }
        }

        doLast {
            delete(layout.buildDirectory.dir("minecraft/minecraft"))
            delete(layout.buildDirectory.dir("minecraft/minecraft_server"))

            copy {
                from(layout.buildDirectory.dir("minecraft/temp"))
                into(layout.buildDirectory.dir("minecraft"))
            }

            delete(layout.buildDirectory.dir("minecraft/temp"))
        }
    }

    val reobfuscate by registering(Exec::class) {
        dependsOn(recompile)

        inputs.dir(layout.buildDirectory.dir("classes"))
        outputs.dir(layout.buildDirectory.dir("reobf"))

        workingDir(config.mcpDir.get().asFile)

        commandLine(
            command("reobfuscate").apply {
                if (!config.serverEnabled.get()) {
                    add("--client")
                }
            }
        )
    }

    val reobfClientZip by registering(Zip::class) {
        dependsOn(reobfuscate)

        from(layout.buildDirectory.dir("reobf/minecraft"))
        from(layout.projectDirectory.dir("src/client/resources"))

        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    }

    val reobfServerZip by registering(Zip::class) {
        dependsOn(reobfuscate)

        archiveClassifier.set("server")

        from(layout.buildDirectory.dir("reobf/minecraft_server"))

        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    }

    named("assemble") {
        dependsOn(reobfClientZip, reobfServerZip)
    }
}
