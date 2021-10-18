val pluginBuild = gradle.includedBuild("plugin")

val publish by tasks.registering {
    dependsOn(pluginBuild.task(":publish"))
}

val clean by tasks.registering(Delete::class) {
    dependsOn(pluginBuild.task(":clean"))

    delete(layout.buildDirectory)
}
