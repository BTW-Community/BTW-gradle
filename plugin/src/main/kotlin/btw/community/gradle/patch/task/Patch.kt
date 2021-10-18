package btw.community.gradle.patch.task

import com.cloudbees.diff.ContextualPatch
import com.cloudbees.diff.ContextualPatch.PatchStatus
import com.cloudbees.diff.PatchException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

fun patch(patchFile: File, patchTarget: File): Boolean {
    val patch = ContextualPatch.create(patchFile, patchTarget)

    patch.patch(false).forEach { report ->
        if (report.status == PatchStatus.Patched) {
            report.originalBackupFile.delete()
        } else {
            println("Failed to apply: $patchFile")
            if (report.failure is PatchException) {
                println("    ${report.failure.message}")
            } else {
                report.failure.printStackTrace()
            }

            return false
        }
    }

    return true
}

class PatchAction(
    private val target: File,
    private val patches: Iterable<File>
) {
    fun execute() {
        var success = true

        patches
            .filter { it.path.endsWith(".patch") }
            .forEach { file ->
                success = patch(file, target) && success
            }

        if (!success) {
            throw GradleException("One or more patches failed to apply, see log for details")
        }
    }
}

// TODO add binary diff with jbsdiff and jbspatch
// TODO allow for single file patching
abstract class Patch : DefaultTask() {
    @get:InputDirectory
    abstract val patchDir: DirectoryProperty

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    @TaskAction
    fun applyPatches() {
        // TODO make incremental with InputChanges

        val patches = patchDir.get().asFileTree.files
        val target = targetDir.get().asFile

        PatchAction(target, patches.asIterable()).execute()
    }

    fun using(patchPath: Any): Patch {
        patchDir.fileValue(project.file(patchPath))

        return this
    }

    fun into(targetPath: Any): Patch {
        targetDir.fileValue(project.file(targetPath))

        return this
    }
}
