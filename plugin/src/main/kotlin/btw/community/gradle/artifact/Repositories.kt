package btw.community.gradle.artifact

import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

fun RepositoryHandler.btwGradle() {
    this.maven {
        url = URI("https://BTW-community.github.io/BTW-gradle/")
    }

    this.ivy {
        url = URI("https://github.com/BTW-Community")
        patternLayout {
            artifact("BTW-Public/archive/refs/tags/[revision].[ext]")
        }
        metadataSources { artifact() }
        content {
            includeModule("btw.community", "btw")
        }
    }
}

fun RepositoryHandler.mojang() {
    this.ivy {
        url = URI("https://launcher.mojang.com/v1/objects")
        patternLayout {
            artifact("f9ae3f651319151ce99a0bfad6b34fa16eb6775f/[classifier].jar")
            artifact("465378c9dc2f779ae1d6e8046ebc46fb53a57968/[classifier].jar")
        }
        metadataSources { artifact() }
        content {
            includeVersion("mojang", "minecraft", "1.5.2")
        }
    }
}
