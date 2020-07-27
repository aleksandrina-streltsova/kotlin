package org.jetbrains.kotlin.tools.projectWizard.wizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.core.service.*

object Services {
    val IDEA_INDEPENDENT_SERVICES: List<IdeaIndependentWizardService> = listOf(
        ProjectImportingWizardServiceImpl(),
        OsFileSystemWizardService(),
        BuildSystemAvailabilityWizardServiceImpl(),
        DummyFileFormattingService(),
        KotlinVersionProviderServiceImpl(),
        RunConfigurationsServiceImpl(),
        SettingSavingWizardServiceImpl(),
        VelocityTemplateEngineServiceImpl()
    )
}