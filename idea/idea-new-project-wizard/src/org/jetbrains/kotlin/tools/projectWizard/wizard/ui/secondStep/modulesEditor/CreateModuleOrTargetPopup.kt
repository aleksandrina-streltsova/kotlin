package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.popup.PopupFactoryImpl
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.topmostHmppSourcesetAncestor
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.fullTextHtml
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.icon
import javax.swing.Icon

class CreateModuleOrTargetPopup private constructor(
    private val target: Module?,
    private val allowMultiplatform: Boolean,
    private val allowSinglepaltformJs: Boolean,
    private val allowAndroid: Boolean,
    private val allowIos: Boolean,
    private val createTargetOrSourceSet: (ModuleConfigurator) -> Unit,
    private val createModule: (ModuleConfigurator) -> Unit
) {

    private fun TargetConfigurator.needToShow(): Boolean {
        val allTargetConfigurators = if (target?.kind == ModuleKind.hmppSourceSet) {
            val topmostAncestor = target.topmostHmppSourcesetAncestor
            topmostAncestor?.subModules?.withAllSubModules().orEmpty()
        } else {
            target?.subModules ?: return false
        }.mapNotNull { it.configurator as? TargetConfigurator }
        return if (target.kind == ModuleKind.hmppSourceSet) {
            canCoexistInHmppSourceSetWith(allTargetConfigurators)
        } else {
            canCoexistsWith(allTargetConfigurators)
        }
    }

    private fun DisplayableSettingItem.needToShow(): Boolean = when (this) {
        is TargetConfigurator -> needToShow()
        is SourceSetTemplateConfigurator, is HmppSourceSetConfigurator -> true
        is TargetConfiguratorGroupWithSubItems -> subItems.any { it.needToShow() }
        else -> false
    }

    private inner class ChooseModuleOrMppModuleStep : BaseListPopupStep<ModuleConfigurator>(
        "Module Type",
        buildList {
            if (allowMultiplatform) +MppModuleConfigurator
            +JvmSinglePlatformModuleConfigurator
            if (allowAndroid) +AndroidSinglePlatformModuleConfigurator
            if (allowSinglepaltformJs) +JsSingleplatformModuleConfigurator
            if (allowIos) +IOSSinglePlatformModuleConfigurator
        }
    ) {
        override fun getIconFor(value: ModuleConfigurator): Icon = value.icon
        override fun getTextFor(value: ModuleConfigurator): String = value.fullTextHtml

        override fun onChosen(selectedValue: ModuleConfigurator?, finalChoice: Boolean): PopupStep<*>? =
            when (selectedValue) {
                null -> PopupStep.FINAL_CHOICE
                else -> {
                    createModule(selectedValue)
                    PopupStep.FINAL_CHOICE
                }
            }
    }

    private inner class ChooseTargetTypeStep(
        targetConfiguratorGroup: TargetConfiguratorGroupWithSubItems,
        showTitle: Boolean
    ) : BaseListPopupStep<DisplayableSettingItem>(
        KotlinNewProjectWizardBundle.message("module.kind.target.or.source.set").takeIf { showTitle },
        targetConfiguratorGroup.subItems.filter { it.needToShow() }
    ) {
        override fun getIconFor(value: DisplayableSettingItem): Icon? = when (value) {
            is DisplayableTargetConfiguratorGroup -> value.icon
            is ModuleConfigurator -> value.icon ?: AllIcons.Nodes.Module
            else -> null
        }

        override fun hasSubstep(selectedValue: DisplayableSettingItem?): Boolean =
            selectedValue is TargetConfiguratorGroupWithSubItems

        override fun isAutoSelectionEnabled(): Boolean = true

        override fun getTextFor(value: DisplayableSettingItem): String = value.fullTextHtml

        override fun onChosen(selectedValue: DisplayableSettingItem?, finalChoice: Boolean): PopupStep<*>? {
            when {
                finalChoice && selectedValue is ModuleConfigurator -> createTargetOrSourceSet(selectedValue)
                selectedValue is TargetConfiguratorGroupWithSubItems ->
                    return ChooseTargetTypeStep(selectedValue, showTitle = false)
            }
            return PopupStep.FINAL_CHOICE
        }
    }

    private fun create(): ListPopup? = when {
        target?.kind == ModuleKind.multiplatform || target?.kind == ModuleKind.hmppSourceSet -> ChooseTargetTypeStep(
            TargetConfigurationGroups.FIRST,
            showTitle = true
        )
        allowMultiplatform || allowAndroid || allowIos -> ChooseModuleOrMppModuleStep()
        else -> {
            createModule(JvmSinglePlatformModuleConfigurator)
            null
        }
    }?.let { PopupFactoryImpl.getInstance().createListPopup(it) }

    companion object {
        fun create(
            target: Module?,
            allowMultiplatform: Boolean,
            allowSinglepaltformJs: Boolean,
            allowAndroid: Boolean,
            allowIos: Boolean,
            createTargetOrSourceSet: (ModuleConfigurator) -> Unit,
            createModule: (ModuleConfigurator) -> Unit
        ) = CreateModuleOrTargetPopup(
            target = target,
            allowMultiplatform = allowMultiplatform,
            allowSinglepaltformJs = allowSinglepaltformJs,
            allowAndroid = allowAndroid,
            allowIos = allowIos,
            createTargetOrSourceSet = createTargetOrSourceSet,
            createModule = createModule
        ).create()
    }
}

