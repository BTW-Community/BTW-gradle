package btw.community.gradle.config

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject

open class BtwGradleExtension @Inject constructor(val project: Project) {
    val jinputVersion = "2.0.5"
    val lwjglVersion = "2.9.1"

    val taskGroup = "BTW-gradle"

    private fun prop(name: String, default: String = ""): String =
        project.findProperty(name) as? String ?: default

    val serverEnabled = project.objects
        .property(Boolean::class.java)
        .convention(true)

    val jdkHome = project.objects
        .property(File::class.java)
        .convention(File(System.getProperty("java.home")))

    val mcpDir = project.objects
        .directoryProperty()
        .convention(project.layout.buildDirectory.dir("mcp"))

    val minecraftDir = project.objects
        .directoryProperty()
        .convention(project.layout.projectDirectory.dir(".minecraft"))

    val modInfo = project.objects.property(ModInfo::class.java).convention(
        ModInfo(
            modid = prop("modid", "INVALID"),
            name = prop("name", "INVALID"),

            version = project.version.toString()
        ).apply {
            mcversion = prop("mcversion", "1.5.2")
            btwversion = prop("btwversion", "CE-1.0.1")

            description = prop("description")
            url = prop("url")
            credits = prop("credits")

            authorList = (project.findProperty("authorList") as? String)
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: listOf()
        }
    )

    fun modInfo(action: Action<ModInfo>): Property<ModInfo> =
        modInfo.value(modInfo.get().apply { action.execute(this) })
}
