rootProject.name = "btw-gradle"

gradle.rootProject {
    group = "btw.community.gradle"
}

include(":example")
includeBuild("plugin")
