package btw.community.gradle.util

import org.gradle.api.Project

/*fun Any.toDebugString(): String = this::class.memberProperties
    .joinToString(", ") { "${ it.name }=${ it.getter.call(this).toString() }" }
    .let { "${this::class.simpleName}(${ it })" };*/

inline fun <reified T> retrieveExtension(project: Project, name: String): T =
    project.extensions.findByName(name) as? T ?: project.extensions.create(name, T::class.java)
