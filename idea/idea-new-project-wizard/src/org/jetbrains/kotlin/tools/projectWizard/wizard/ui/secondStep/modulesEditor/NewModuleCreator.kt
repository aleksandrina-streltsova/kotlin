package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.SourceSetTemplateConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.TargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*

class NewModuleCreator {
    private fun suggestName(@NonNls name: String, modules: List<Module>): String {
        val names = modules.map(Module::name).toSet()
        if (name !in names) return name
        var index = 1
        while ("${name}_$index" in names) {
            index++
        }
        return "${name}_$index"
    }

    private fun newTargetOrSourceSet(
        configurator: ModuleConfigurator,
        allSubModules: List<Module>
    ): Module {
        val name = configurator.suggestedModuleName ?: if (configurator is TargetConfigurator) configurator.moduleType.name else "module"
        return when (configurator) {
            is SourceSetTemplateConfigurator -> configurator.createSourceSetTemplate(name, allSubModules, this::suggestName)
            else -> MultiplatformTargetModule(suggestName(name, allSubModules), configurator, createDefaultSourcesets())
        }
    }

    fun create(
        target: Module?,
        allowMultiplatform: Boolean,
        allowSinglepaltformJs: Boolean,
        allowAndroid: Boolean,
        allowIos: Boolean,
        allModules: List<Module>,
        createModule: (Module) -> Unit
    ) = CreateModuleOrTargetPopup.create(
        target = target,
        allowMultiplatform = allowMultiplatform,
        allowSinglepaltformJs = allowSinglepaltformJs,
        allowAndroid = allowAndroid,
        allowIos = allowIos,
        createTargetOrSourceSet = { configurator ->
            val mppModule = target?.topmostHmppSourcesetAncestor?.parent ?: target
            createModule(newTargetOrSourceSet(configurator, mppModule?.subModules?.withAllSubModules().orEmpty()))
        },
        createModule = { configurator ->
            val name = suggestName(configurator.suggestedModuleName ?: "module", allModules)
            val sourcesets = when (configurator.moduleKind) {
                ModuleKind.multiplatform -> emptyList()
                else -> SourcesetType.values().map { sourcesetType ->
                    Sourceset(
                        sourcesetType,
                        dependencies = emptyList()
                    )
                }
            }
            val createdModule = Module(
                name,
                configurator,
                template = null,
                sourcesets = sourcesets,
                subModules = emptyList()
            )
            createModule(createdModule)
        }
    )
}