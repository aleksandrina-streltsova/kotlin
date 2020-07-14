/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType

object TargetConfigurationGroups {
    val JS = FinalTargetConfiguratorGroup(
        ModuleType.js.projectTypeName,
        KotlinIcons.Wizard.JS,
        listOf(
            JsBrowserTargetConfigurator,
            JsNodeTargetConfigurator
        )
    )

    object NATIVE {
        val LINUX = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.linux"),
            KotlinIcons.Wizard.LINUX,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxX64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxArm32Hfp),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxMips32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.linuxMipsel32)
            )
        )

        val WINDOWS = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.windows.mingw"),
            KotlinIcons.Wizard.WINDOWS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.mingwX64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.mingwX86)
            )
        )

        val MAC = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.macos"),
            KotlinIcons.Wizard.MAC_OS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.macosX64)
            )
        )

        val IOS = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.ios"),
            KotlinIcons.Wizard.IOS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosArm32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosX64)
            )
        )

        val WATCHOS = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.watchos"),
            KotlinIcons.Wizard.MAC_OS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.watchosArm32),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.watchosArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.watchosX86)
            )
        )

        val TVOS = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.tvos"),
            KotlinIcons.Wizard.MAC_OS,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.tvosArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.tvosX64)
            )
        )

        val ANDROID_NATIVE = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.android.native"),
            KotlinIcons.Wizard.ANDROID,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.androidNativeArm64),
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.androidNativeArm32)
            )
        )

        val WEB_ASSEMBLY = FinalTargetConfiguratorGroup(
            KotlinNewProjectWizardBundle.message("module.configuration.group.web.assembly"),
            KotlinIcons.Wizard.NATIVE,
            listOf(
                RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.wasm32)
            )
        )

        val ALL = StepTargetConfiguratorGroup(
            ModuleType.native.projectTypeName,
            ModuleType.native,
            listOf(
                NativeForCurrentSystemTarget,
                LINUX,
                WINDOWS,
                MAC,
                IOS,
                WATCHOS,
                TVOS,
                ANDROID_NATIVE,
                WEB_ASSEMBLY
            )
        )
    }

    val SOURCE_SET = FinalTargetConfiguratorGroup(
        KotlinNewProjectWizardBundle.message("module.configuration.group.sourceset"),
        KotlinIcons.SMALL_LOGO,
        listOf(
            IOSSourceSetTemplateConfigurator,
            WatchOSSourceSetTemplateConfigurator,
            TvOSSourceSetTemplateConfigurator,
            EmptySourceSetTemplateConfigurator
        )
    )
    val FIRST = FirstStepTargetConfiguratorGroup(
        listOf(
            CommonTargetConfigurator,
            JvmTargetConfigurator,
            NATIVE.ALL,
            JS,
            AndroidTargetConfigurator,
            SOURCE_SET
        )
    )
}