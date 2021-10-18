package btw.community.gradle.config

// https://mcforge.readthedocs.io/en/1.13.x/gettingstarted/structuring/
// TODO add updateJSON, logoFile, screenshots, parent, requiredMods, dependencies, dependants
open class ModInfo(
    var modid: String,
    var name: String,
    var version: String
) {
    var mcversion: String = "1.5.2"
    var btwversion: String = "CE-1.0.1"
    var authorList: List<String> = listOf()
    var description: String = ""
    var url: String = ""
    var credits: String = ""

    fun toJSON() = """
        [{
            "modid": "$modid",
            "name": "$name",

            "version": "$version",
            "mcversion": "$mcversion",
            "btwversion": "$btwversion",

            "authorList": [${authorList.joinToString(", ") { "\"${it}\"" }}],
            "description": "$description",
            "url": "$url",
            "credits": "$credits",
        }]
    """.trimIndent()
}
