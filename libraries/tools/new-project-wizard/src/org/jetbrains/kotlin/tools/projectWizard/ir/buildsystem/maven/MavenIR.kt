/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.maven

import org.jetbrains.kotlin.tools.projectWizard.core.NonNls
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.RepositoryWrapper
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository

interface MavenIR : BuildSystemIR {
    fun MavenPrinter.renderMaven()

    override fun BuildFilePrinter.render() {
        if (this is MavenPrinter) renderMaven()
    }
}

data class PluginRepositoryMavenIR(
    override val repository: Repository
) : MavenIR, RepositoryWrapper {
    override fun MavenPrinter.renderMaven() {
        node("pluginRepository") {
            singleLineNode("id") { +repository.idForMaven }
            singleLineNode("url") { +repository.url }
        }
    }
}

data class MavenPropertyIR(
    @NonNls val name: String,
    @NonNls val value: String
) : MavenIR {
    override fun MavenPrinter.renderMaven() {
        singleLineNode(name) { +value }
    }
}


