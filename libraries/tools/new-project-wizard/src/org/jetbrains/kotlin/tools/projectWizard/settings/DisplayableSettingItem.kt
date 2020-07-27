package org.jetbrains.kotlin.tools.projectWizard.settings

import org.jetbrains.kotlin.tools.projectWizard.core.Nls

interface DisplayableSettingItem {
    @get:Nls
    val text: String

    @get:Nls
    val greyText: String?
        get() = null
}
