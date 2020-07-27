package org.jetbrains.kotlin.tools.projectWizard.core

import java.lang.Character.isLetterOrDigit
import java.lang.Character.isDigit

actual typealias Nls = org.jetbrains.annotations.Nls
actual typealias NonNls = org.jetbrains.annotations.NonNls

actual fun Char.isLetterOrDigit(): Boolean = isLetterOrDigit(this)
actual fun Char.isDigit(): Boolean = isDigit(this)
