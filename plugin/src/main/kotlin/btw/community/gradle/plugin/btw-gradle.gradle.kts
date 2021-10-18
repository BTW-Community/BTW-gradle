package btw.community.gradle.plugin

import btw.community.gradle.config.BtwGradleExtension
import btw.community.gradle.util.retrieveExtension

val config = retrieveExtension<BtwGradleExtension>(project, "btw")

apply { type(BtwGradleProjectPlugin::class.java) }
apply { type(BtwGradleMcpPlugin::class.java) }

object {
    val info by tasks.registering {
        group = config.taskGroup
        description = "Print out the addon information"

        inputs.property("modInfo", config.modInfo)

        doLast {
            println(config.modInfo.get().toJSON())
        }
    }
}
