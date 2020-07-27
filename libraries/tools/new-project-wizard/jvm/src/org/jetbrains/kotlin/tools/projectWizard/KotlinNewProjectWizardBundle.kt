package org.jetbrains.kotlin.tools.projectWizard

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.KotlinNewProjectWizardBundle"

actual object KotlinNewProjectWizardBundle : AbstractBundle(BUNDLE) {
    @JvmStatic
    actual fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}