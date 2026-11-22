/*
 * Print APKs task configuration
 * Creates task to print the generated APK path
 */

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

internal fun Project.configurePrintApksTask(
    extension: AndroidComponentsExtension<*, *, *>,
) {
    extension.onVariants { variant ->
        tasks.register("print${variant.name.replaceFirstChar { it.uppercase() }}Apks") {
            group = "help"
            description = "Prints the APK path for ${variant.name} variant"

            val apkDir = variant.artifacts.get(SingleArtifact.APK)
            doLast {
                println("APK for ${variant.name}: ${apkDir.get().asFile.absolutePath}")
            }
        }
    }
}
