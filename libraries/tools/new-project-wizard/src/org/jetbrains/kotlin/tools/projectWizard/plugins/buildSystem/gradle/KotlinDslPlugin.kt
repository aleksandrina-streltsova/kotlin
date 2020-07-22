package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildFileData
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemData
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

class KotlinDslPlugin(context: Context) : GradlePlugin(context) {
    override val path = PATH

    companion object {
        private const val PATH = "buildSystem.gradle.kotlinDsl"

        val addBuildSystemData by addBuildSystemData(
            PATH,
            BuildSystemData(
                type = BuildSystemType.GradleKotlinDsl,
                buildFileData = BuildFileData(
                    createPrinter = { GradlePrinter(GradlePrinter.GradleDsl.KOTLIN) },
                    buildFileName = "build.gradle.kts"
                )
            )
        )
    }

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                addBuildSystemData,
            )
}