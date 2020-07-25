package org.jetbrains.kotlin.tools.projectWizard.plugins


import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.PomIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

class StructurePlugin(context: Context) : Plugin(context) {
    override val path = PATH

    companion object {
        private const val PATH = "structure"

        private val ALLOWED_SPECIAL_CHARS_IN_GROUP_ID = Module.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES + '.'
        private val ALLOWED_SPECIAL_CHARS_IN_ARTIFACT_ID = Module.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES
        private val ALLOWED_SPECIAL_CHARS_IN_VERSION = setOf('_', '-', '.')

        val projectPath by pathSetting(
            KotlinNewProjectWizardBundle.message("plugin.structure.setting.location"),
            GenerationPhase.FIRST_STEP,
            PATH
        ) {
            defaultValue = value(Paths.get("."))

            validateOnProjectCreation = false

            validate { path ->
                if (!Files.exists(path)) return@validate ValidationResult.OK
                ValidationResult.create(Files.list(path).none { true }) {
                    KotlinNewProjectWizardBundle.message("plugin.structure.setting.location.error.is.not.empty")
                }
            }
        }
        val name by stringSetting(
            KotlinNewProjectWizardBundle.message("plugin.structure.setting.name"),
            GenerationPhase.FIRST_STEP,
            PATH
        ) {
            shouldNotBeBlank()
            validate(StringValidators.shouldBeValidIdentifier(title, Module.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES))
        }

        val groupId by stringSetting(
            KotlinNewProjectWizardBundle.message("plugin.structure.setting.group.id"),
            GenerationPhase.FIRST_STEP,
            PATH
        ) {
            isSavable = true
            shouldNotBeBlank()
            validate(StringValidators.shouldBeValidIdentifier(title, ALLOWED_SPECIAL_CHARS_IN_GROUP_ID))
        }
        val artifactId by stringSetting(
            KotlinNewProjectWizardBundle.message("plugin.structure.setting.artifact.id"),
            GenerationPhase.FIRST_STEP,
            PATH
        ) {
            shouldNotBeBlank()
            validate(StringValidators.shouldBeValidIdentifier(title, ALLOWED_SPECIAL_CHARS_IN_ARTIFACT_ID))
        }
        val version by stringSetting(
            KotlinNewProjectWizardBundle.message("plugin.structure.setting.version"),
            GenerationPhase.FIRST_STEP,
            PATH
        ) {
            shouldNotBeBlank()
            validate(StringValidators.shouldBeValidIdentifier(title, ALLOWED_SPECIAL_CHARS_IN_VERSION))
            defaultValue = value("1.0-SNAPSHOT")
        }
        val createProjectDir by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            withAction {
                service<FileSystemWizardService>().createDirectory(StructurePlugin.projectPath.settingValue)
            }
        }
    }

    override val settings: List<PluginSetting<*, *>> =
        listOf(
            projectPath,
            name,
            groupId,
            artifactId,
            version
        )
    override val pipelineTasks: List<PipelineTask> =
        listOf(createProjectDir)
    override val properties: List<Property<*>> = listOf()
}

val Reader.projectPath
    get() = StructurePlugin.projectPath.settingValue

val Reader.projectName
    get() = StructurePlugin.name.settingValue


fun Writer.pomIR() = PomIR(
    artifactId = StructurePlugin.artifactId.settingValue,
    groupId = StructurePlugin.groupId.settingValue,
    version = Version.fromString(StructurePlugin.version.settingValue)
)
