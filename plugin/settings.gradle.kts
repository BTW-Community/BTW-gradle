val name: String by settings
val group: String by settings
val version: String by settings
val description: String by settings

rootProject.name = name

gradle.rootProject {
    group = group
    version = version
    description = description
}
