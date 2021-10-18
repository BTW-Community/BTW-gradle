package btw.community.gradle.patch.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

// TODO add binary diff with jbsdiff and jbspatch
// TODO allow for single file diffing
abstract class Diff : DefaultTask() {
    companion object {
        const val NUM_CONTEXT_LINES = 3
    }

    @get:InputDirectory
    abstract val baseDir: DirectoryProperty

    @get:InputDirectory
    abstract val targetDir: DirectoryProperty

    @get:OutputDirectory
    abstract val patchDir: DirectoryProperty

    @TaskAction
    fun makePatches() {
        // TODO make incremental with InputChanges

        project.delete(patchDir.get().asFileTree.files)

        val base = object {
            val dir = baseDir.get()
            val files = dir.asFileTree.files
                .map { it.relativeTo(dir.asFile) }
        }

        val target = object {
            val dir = targetDir.get()
            val files = dir.asFileTree.files
                .map { it.relativeTo(dir.asFile) }
        }

        val all = base.files.union(target.files)

        all.forEach { relative ->
            val original = object {
                val file = base.dir.file(relative.path).asFile
                val exists = file.exists()
                val path = if (exists) relative.path else "/dev/null"
                fun reader() = if (exists) file.reader() else "".reader()
            }
            val modified = object {
                val file = target.dir.file(relative.path).asFile
                val exists = file.exists()
                val path = if (exists) relative.path else "/dev/null"
                fun reader() = if (exists) file.reader() else "".reader()
            }

            val diff = com.cloudbees.diff.Diff.diff(
                original.reader(),
                modified.reader(),
                false
            )

            if (!diff.isEmpty()) {
                val patch = patchDir.get().file("${relative.path}.patch").asFile
                patch.parentFile.mkdirs()
                patch.createNewFile()

                val unified: String = diff.toUnifiedDiff(
                    original.path, modified.path,
                    original.reader(), modified.reader(),
                    NUM_CONTEXT_LINES
                ).replace("\r\n?".toRegex(), "\n")

                patch.printWriter().use { out ->
                    out.write(unified)
                }
            }
        }
    }

    fun from(basePath: Any): Diff {
        baseDir.fileValue(project.file(basePath))

        return this
    }

    fun into(targetPath: Any): Diff {
        targetDir.fileValue(project.file(targetPath))

        return this
    }

    fun generating(patchPath: Any): Diff {
        patchDir.fileValue(project.file(patchPath))

        return this
    }
}
