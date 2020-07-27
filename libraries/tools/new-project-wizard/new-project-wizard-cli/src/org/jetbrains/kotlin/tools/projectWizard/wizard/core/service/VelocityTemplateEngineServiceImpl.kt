package org.jetbrains.kotlin.tools.projectWizard.wizard.core.service

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.RuntimeServices
import org.apache.velocity.runtime.log.LogChute
import org.jetbrains.kotlin.tools.projectWizard.core.service.IdeaIndependentWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.TemplateEngineService
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import java.io.StringWriter

class VelocityTemplateEngineServiceImpl : TemplateEngineService(), IdeaIndependentWizardService {
    override fun renderTemplate(template: FileTemplateDescriptor, data: Map<String, Any?>): String {
        val templateText = getTemplateText(template)
        val context = VelocityContext().apply {
            data.forEach { (key, value) -> put(key, value) }
        }
        return StringWriter().use { writer ->
            runVelocityActionWithoutLogging { Velocity.evaluate(context, writer, "", templateText) }
            writer.toString()
        }
    }


    private fun runVelocityActionWithoutLogging(action: () -> Unit) {
        val initialLogger = Velocity.getProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM)
        Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, DoNothingVelocityLogger)
        action()
        if (initialLogger != null) {
            Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, initialLogger)
        }
    }

    private object DoNothingVelocityLogger : LogChute {
        override fun isLevelEnabled(level: Int): Boolean = false
        override fun init(rs: RuntimeServices?) = Unit
        override fun log(level: Int, message: String?) = Unit
        override fun log(level: Int, message: String?, t: Throwable?) = Unit
    }
}