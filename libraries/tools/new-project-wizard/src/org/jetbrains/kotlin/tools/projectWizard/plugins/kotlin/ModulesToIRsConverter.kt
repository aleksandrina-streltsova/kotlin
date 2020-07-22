package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import java.nio.file.Path

data class ModulesToIrConversionData(
    val rootModules: List<Module>,
    val projectPath: Path,
    val projectName: String,
    val kotlinVersion: WizardKotlinVersion,
    val buildSystemType: BuildSystemType,
    val pomIr: PomIR
) {
    val allModules = rootModules.withAllSubModules()
    val isSingleRootModuleMode = rootModules.size == 1

    val moduleByPath = rootModules.withAllSubModules(includeSourcesets = true).associateBy(Module::path)
}

private data class ModulesToIrsState(
    val parentPath: Path,
    val parentModuleHasTransitivelySpecifiedKotlinVersion: Boolean
)

private fun ModulesToIrsState.stateForSubModule(currentModulePath: Path) =
    copy(
        parentPath = currentModulePath,
        parentModuleHasTransitivelySpecifiedKotlinVersion = true
    )

class ModulesToIRsConverter(
    val data: ModulesToIrConversionData
) {

    // TODO get rid of mutable state
    private val rootBuildFileIrs = mutableListOf<BuildSystemIR>()

    // check if we need to flatten our module structure to a single-module
    // as we always have a root module in the project
    // which is redundant for a single module projects
    private val needFlattening: Boolean
        get() {
            if ( // We want to have root build file for android or ios projects
                data.allModules.any { it.configurator.requiresRootBuildFile }
            ) return false
            return data.isSingleRootModuleMode
        }

    private val irsToAddToModules = hashMapOf<Module, MutableList<BuildSystemIR>>()
    private val moduleToBuildFile = hashMapOf<Module, BuildFileIR>()

    private fun calculatePathForModule(module: Module, rootPath: Path) = when {
        needFlattening && module.isRootModule -> data.projectPath
        else -> rootPath / module.name
    }

    fun Writer.createBuildFiles(): TaskResult<List<BuildFileIR>> = with(data) {
        val needExplicitRootBuildFile = !needFlattening
        val initialState = ModulesToIrsState(projectPath, parentModuleHasTransitivelySpecifiedKotlinVersion = false)

        val parentModuleHasKotlinVersion = allModules.any { module ->
            module.configurator == AndroidSinglePlatformModuleConfigurator
        }

        rootModules.mapSequence { module ->
            createBuildFileForModule(
                module,
                initialState.copy(parentModuleHasTransitivelySpecifiedKotlinVersion = parentModuleHasKotlinVersion)
            )
        }.map { it.flatten() }.map { buildFiles ->
            if (needExplicitRootBuildFile) buildFiles + createRootBuildFile()
            else buildFiles
        }.map { buildFiles ->
            buildFiles.map { buildFile ->
                val irs = buildFile.fromModules.flatMap { irsToAddToModules[it]?.toList() ?: emptyList() }
                buildFile.withIrs(irs)
            }
        }
    }

    private fun createRootBuildFile(): BuildFileIR = with(data) {
        BuildFileIR(
            projectName,
            projectPath,
            RootFileModuleStructureIR(persistentListOf()),
            emptyList(),
            pomIr,
            rootBuildFileIrs.toPersistentList()
        )
    }


    private fun Writer.createBuildFileForModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = when (val configurator = module.configurator) {
        is MppModuleConfigurator -> createMultiplatformModule(module, state)
        is SinglePlatformModuleConfigurator -> createSinglePlatformModule(module, configurator, state)
        else -> Success(emptyList())
    }

    private fun Writer.createSinglePlatformModule(
        module: Module,
        configurator: SinglePlatformModuleConfigurator,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = computeM {
        val modulePath = calculatePathForModule(module, state.parentPath)
        val (moduleDependencies) = module.dependencies.mapCompute { dependency ->
            val to = data.moduleByPath.getValue(dependency.path)
            val (dependencyType) = ModuleDependencyType.getPossibleDependencyType(module, to)
                .toResult { InvalidModuleDependencyError(module, to) }

            with(dependencyType) {
                @Suppress("DEPRECATION")
                with(unsafeSettingWriter) {
                    runArbitraryTask(
                        module,
                        to,
                        to.path.considerSingleRootModuleMode(data.isSingleRootModuleMode).asPath(),
                        data
                    ).ensure()
                }
                irsToAddToModules.getOrPut(to) { mutableListOf() } += createToIRs(module, to, data).get()
                createDependencyIrs(module, to, data)
            }
        }.sequence().map { it.flatten() }
        mutateProjectStructureByModuleConfigurator(module, modulePath)
        val buildFileIR = run {
            if (!configurator.needCreateBuildFile) return@run null
            val dependenciesIRs = buildPersistenceList<BuildSystemIR> {
                +moduleDependencies
                with(configurator) { +createModuleIRs(this@createSinglePlatformModule, data, module) }
                addIfNotNull(
                    configurator.createStdlibType(data, module)?.let { stdlibType ->
                        KotlinStdlibDependencyIR(
                            type = stdlibType,
                            isInMppModule = false,
                            kotlinVersion = data.kotlinVersion,
                            dependencyType = DependencyType.MAIN
                        )
                    }
                )
            }

            val moduleIr = SingleplatformModuleIR(
                if (modulePath == data.projectPath) data.projectName else module.name,
                modulePath,
                dependenciesIRs,
                module.template,
                module,
                module.sourcesets.map { sourceset ->
                    SingleplatformSourcesetIR(
                        sourceset.sourcesetType,
                        modulePath / Defaults.SRC_DIR / sourceset.sourcesetType.name,
                        persistentListOf(),
                        sourceset
                    )
                }
            )
            BuildFileIR(
                module.name,
                modulePath,
                SingleplatformModulesStructureWithSingleModuleIR(
                    moduleIr,
                    persistentListOf()
                ),
                listOf(module),
                data.pomIr.copy(artifactId = module.name),
                createBuildFileIRs(module, state)
            ).also {
                moduleToBuildFile[module] = it
            }
        }

        module.subModules.mapSequence { subModule ->
            createBuildFileForModule(
                subModule,
                state.stateForSubModule(modulePath)
            )
        }.map { it.flatten() }
            .map { children ->
                buildFileIR?.let { children + it } ?: children
            }
    }

    private fun Writer.createMultiplatformModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = with(data) {
        val modulePath = calculatePathForModule(module, state.parentPath)
        mutateProjectStructureByModuleConfigurator(module, modulePath)
        // hmpp source sets must be defined after other source sets
        val allSubModules = module.subModules.withAllSubModules().sortedBy { it.kind == ModuleKind.hmppSourceSet }
        val targetIrs = allSubModules.flatMap { subModule ->
            when {
                moduleIsSourceSetWithShortcut(subModule) != null -> with(subModule.configurator as HmppSourceSetConfigurator) {
                    createSourceSetIrs(subModule)
                }
                moduleIsPartOfSourceSetWithShortcut(subModule) || subModule.kind == ModuleKind.hmppSourceSet -> emptyList()
                else -> with(subModule.configurator as TargetConfigurator) { createTargetIrs(subModule) }
            }
        }

        val targetModuleIrs = allSubModules.map { target -> createTargetModule(module, target, modulePath) }

        return BuildFileIR(
            projectName,
            modulePath,
            MultiplatformModulesStructureIR(
                targetIrs,
                targetModuleIrs,
                persistentListOf()
            ),
            allSubModules + module,
            pomIr,
            buildPersistenceList {
                +createBuildFileIRs(module, state)
                allSubModules.forEach { +createBuildFileIRs(it, state) }
            }
        ).also { buildFile ->
            moduleToBuildFile[module] = buildFile
            allSubModules.forEach { subModule ->
                moduleToBuildFile[subModule] = buildFile
            }
        }.asSingletonList().asSuccess()

    }

    private fun Writer.createTargetModule(mppModule: Module, target: Module, modulePath: Path): MultiplatformModuleIR {
        mutateProjectStructureByModuleConfigurator(target, modulePath)
        val commonModule: Module? = mppModule.subModules.firstOrNull { it.configurator == CommonTargetConfigurator }
        val sourcesetss = target.sourcesets.map { sourceset ->
            val typeName = sourceset.sourcesetType.name.capitalize()
            val sourcesetName = target.name + typeName
            val sourcesetIrs = irsList {
                if (sourceset.sourcesetType == SourcesetType.main) {
                    target.configurator.createStdlibType(data, target)?.let { stdlibType ->
                        +KotlinStdlibDependencyIR(
                            type = stdlibType,
                            isInMppModule = true,
                            kotlinVersion = data.kotlinVersion,
                            dependencyType = DependencyType.MAIN
                        )
                    }
                }
                if (target.kind == ModuleKind.hmppSourceSet) {
                    val parentName = if (target.parent?.kind == ModuleKind.multiplatform) commonModule?.name else target.parent?.name
                    parentName?.let {
                        addRaw {
                            call("dependsOn") { +(parentName + typeName) }
                        }
                    }
                    val childSourceSetNames = target.subModules.filter { it.kind != ModuleKind.hmppSourceSet }.map { it.name + typeName }
                    for (childSourceSetName in childSourceSetNames) {
                        addRaw {
                            call("$childSourceSetName.dependsOn") { +if (dsl == GradlePrinter.GradleDsl.KOTLIN) "this" else "it" }
                        }
                    }
                }
            }
            MultiplatformSourcesetIR(
                sourceset.sourcesetType,
                modulePath / Defaults.SRC_DIR / sourcesetName,
                target.name,
                target.kind == ModuleKind.hmppSourceSet,
                sourcesetIrs.toPersistentList(),
                sourceset
            )
        }
        return MultiplatformModuleIR(
            target.name,
            modulePath,
            with(target.configurator) { createModuleIRs(this@createTargetModule, data, target) }.toPersistentList(),
            target.template,
            target,
            sourcesetss
        )
    }

    private fun Writer.mutateProjectStructureByModuleConfigurator(
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = with(module.configurator) {
        compute {
            rootBuildFileIrs += createRootBuildFileIrs(data)
            runArbitraryTask(data, module, modulePath).ensure()
            TemplatesPlugin.addFileTemplates.execute(createTemplates(data, module, modulePath)).ensure()
            if (this@with is GradleModuleConfigurator) {
                GradlePlugin.settingsGradleFileIRs.addValues(createSettingsGradleIRs(module)).ensure()
            }
        }
    }

    private fun Reader.createBuildFileIRs(
        module: Module,
        state: ModulesToIrsState
    ) = buildPersistenceList<BuildSystemIR> {
        val kotlinPlugin = module.configurator.createKotlinPluginIR(data, module)
            ?.let { plugin ->
                // do not print version for non-root modules for gradle
                val needRemoveVersion = data.buildSystemType.isGradle
                        && state.parentModuleHasTransitivelySpecifiedKotlinVersion
                when {
                    needRemoveVersion -> plugin.copy(version = null)
                    else -> plugin
                }
            }
        addIfNotNull(kotlinPlugin)
        +with(module.configurator) { createBuildFileIRs(this@createBuildFileIRs, data, module) }
    }
}