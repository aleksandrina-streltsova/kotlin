package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetAccessIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.maven.MavenPropertyIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import java.nio.file.Path

interface JvmModuleConfigurator : ModuleConfiguratorWithTests {
    companion object : ModuleConfiguratorSettings() {
        val targetJvmVersion by enumSetting<TargetJvmVersion>(
            KotlinNewProjectWizardBundle.message("module.configurator.jvm.setting.target.jvm.version"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            defaultValue = value(TargetJvmVersion.JVM_1_8)
        }
    }

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> = buildList {
        +super.getConfiguratorSettings()
        +targetJvmVersion
    }
}

enum class TargetJvmVersion(@NonNls val value: String) : DisplayableSettingItem {
    JVM_1_6("1.6"),
    JVM_1_8("1.8"),
    JVM_9("9"),
    JVM_10("10"),
    JVM_11("11"),
    JVM_12("12"),
    JVM_13("13");

    override val text: String
        get() = value
}


interface ModuleConfiguratorWithModuleType : ModuleConfigurator {
    val moduleType: ModuleType
}

val ModuleConfigurator.moduleType: ModuleType?
    get() = safeAs<ModuleConfiguratorWithModuleType>()?.moduleType

object MppModuleConfigurator : ModuleConfigurator {
    override val moduleKind = ModuleKind.multiplatform

    @NonNls
    override val suggestedModuleName = "shared"

    @NonNls
    override val id = "multiplatform"
    override val text = KotlinNewProjectWizardBundle.message("module.configurator.mpp")
    override val canContainSubModules = true

    override fun createKotlinPluginIR(configurationData: ModulesToIrConversionData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.multiplatform,
            version = configurationData.kotlinVersion
        )
}


interface SinglePlatformModuleConfigurator : ModuleConfigurator {
    val needCreateBuildFile: Boolean get() = true
}

object JvmSinglePlatformModuleConfigurator : JvmModuleConfigurator,
    SinglePlatformModuleConfigurator,
    ModuleConfiguratorWithModuleType {
    override val moduleType get() = ModuleType.jvm
    override val moduleKind: ModuleKind get() = ModuleKind.singleplatformJvm

    @NonNls
    override val suggestedModuleName = "jvm"

    @NonNls
    override val id = "JVM Module"
    override val text = KotlinNewProjectWizardBundle.message("module.configurator.jvm")


    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JUNIT4

    override val canContainSubModules = true

    override fun createKotlinPluginIR(configurationData: ModulesToIrConversionData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.jvm,
            version = configurationData.kotlinVersion
        )


    override fun createBuildFileIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> =
        buildList {
            +GradleImportIR("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")

            val targetVersionValue = withSettingsOf(module) {
                reader {
                    JvmModuleConfigurator.targetJvmVersion.reference.settingValue.value
                }
            }
            when {
                configurationData.buildSystemType.isGradle -> {
                    +GradleConfigureTaskIR(
                        GradleByClassTasksAccessIR("KotlinCompile"),
                        irs = listOf(
                            GradleAssignmentIR("kotlinOptions.jvmTarget", GradleStringConstIR(targetVersionValue))
                        )
                    )
                }
                configurationData.buildSystemType == BuildSystemType.Maven -> {
                    +MavenPropertyIR("kotlin.compiler.jvmTarget", targetVersionValue)
                }
            }
        }
}


val ModuleType.defaultTarget
    get() = when (this) {
        ModuleType.jvm -> JvmTargetConfigurator
        ModuleType.js -> JsBrowserTargetConfigurator
        ModuleType.native -> NativeForCurrentSystemTarget
        ModuleType.common -> CommonTargetConfigurator
        ModuleType.android -> AndroidTargetConfigurator
    }

object HmppSourceSetConfigurator : ModuleConfigurator {
    override val id: String = "sourceSet"
    override val text: String = KotlinNewProjectWizardBundle.message("module.configurator.sourceset")
    override val suggestedModuleName: String = "sourceSet"

    override val moduleKind: ModuleKind = ModuleKind.hmppSourceSet
    override val canContainSubModules: Boolean = true

    override fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> =
        GradlePlugin::gradleProperties.addValues("kotlin.mpp.enableGranularSourceSetsMetadata" to "true")

    fun createSourceSetIrs(module: Module): List<BuildSystemIR> = irsList {
        moduleIsSourceSetWithShortcut(module)?.let { shortcut ->
            addRaw("// Create ${shortcut.numberOfTargets} targets for ${shortcut.osName}.")
            addRaw("// Create common source sets: ${module.name}Main and ${module.name}Test.")
            +DefaultTargetConfigurationIR(
                TargetAccessIR(null, shortcut.name, module.name.takeIf { it != shortcut.name }),
                irsList {
                    "binaries" {
                        "framework"  {
                            "baseName" assign const(module.name)
                        }
                    }
                }.toPersistentList()
            )
        }
    }
}

abstract class SourceSetTemplateConfigurator : ModuleConfigurator {
    override val moduleKind: ModuleKind = ModuleKind.hmppSourceSet
    override val id: String = "sourceSet"
    abstract val subModuleTypes: List<ModuleSubType>

    fun createSourceSetTemplate(name: String, modules: List<Module>, suggestName: (String, List<Module>) -> String): Module {
        fun createSubModule(subType: ModuleSubType): Module {
            val configurator = RealNativeTargetConfigurator.configuratorsByModuleType.getValue(subType)
            val subModuleName = suggestName(configurator.suggestedModuleName ?: configurator.moduleType.name, modules)
            return Module(subModuleName, configurator, null, createDefaultSourcesets(), emptyList())
        }
        return Module(
            suggestName(name, modules),
            HmppSourceSetConfigurator,
            null,
            createDefaultSourcesets(),
            subModuleTypes.map { createSubModule(it) }
        )
    }
}

@Suppress("EnumEntryName", "SpellCheckingInspection")
enum class Shortcut(val osName: String, val numberOfTargets: String = "two") {
    ios("iOS") {
        override val targetConfigurators: Set<TargetConfigurator> =
            RealNativeTargetConfigurator.getConfiguratorsByModuleTypes(listOf(ModuleSubType.iosArm64, ModuleSubType.iosX64))
    },
    watchos("watchOS", "three") {
        override val targetConfigurators: Set<TargetConfigurator> =
            RealNativeTargetConfigurator.getConfiguratorsByModuleTypes(
                listOf(ModuleSubType.watchosArm32, ModuleSubType.watchosArm64, ModuleSubType.watchosX86)
            )
    },
    tvos("tvOS") {
        override val targetConfigurators: Set<TargetConfigurator> =
            RealNativeTargetConfigurator.getConfiguratorsByModuleTypes(listOf(ModuleSubType.tvosArm64, ModuleSubType.tvosX64))
    }

    ;

    abstract val targetConfigurators: Set<TargetConfigurator>
}

object EmptySourceSetTemplateConfigurator : SourceSetTemplateConfigurator() {
    override val suggestedModuleName: String? = "sourceSet"
    override val text: String = "Empty sourceSet"
    override val subModuleTypes: List<ModuleSubType> = emptyList()
}

object IOSSourceSetTemplateConfigurator : SourceSetTemplateConfigurator() {
    override val suggestedModuleName: String? = "ios"
    override val text: String = "iOS sourceSet"
    override val subModuleTypes: List<ModuleSubType> = listOf(ModuleSubType.iosArm64, ModuleSubType.iosX64)
}

object WatchOSSourceSetTemplateConfigurator : SourceSetTemplateConfigurator() {
    override val suggestedModuleName: String? = "watchos"
    override val text: String = "watchOS sourceSet"
    override val subModuleTypes: List<ModuleSubType> =
        listOf(ModuleSubType.watchosArm32, ModuleSubType.watchosArm64, ModuleSubType.watchosX86)
}

object TvOSSourceSetTemplateConfigurator : SourceSetTemplateConfigurator() {
    override val suggestedModuleName: String? = "tvos"
    override val text: String = "tvOS sourceSet"
    override val subModuleTypes: List<ModuleSubType> = listOf(ModuleSubType.tvosArm64, ModuleSubType.tvosX64)
}

// module is a source set that can be created with shortcut
fun moduleIsSourceSetWithShortcut(module: Module): Shortcut? {
    if (module.kind == ModuleKind.hmppSourceSet && module.parent?.kind == ModuleKind.multiplatform
        && module.subModules.all { subModule -> subModule.name.startsWith(module.name) }
    ) {
        return module.subModules.map { it.configurator }.toSet().let { subModuleConfigurators ->
            Shortcut.values().firstOrNull { shortcut -> shortcut.targetConfigurators == subModuleConfigurators }
        }
    }
    return null
}

fun moduleIsPartOfSourceSetWithShortcut(module: Module): Boolean = module.parent?.let { moduleIsSourceSetWithShortcut(it) != null } ?: false