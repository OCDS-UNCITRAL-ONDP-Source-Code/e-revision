package com.procurement.revision.infrastructure.service

import com.procurement.revision.application.service.Logger
import org.slf4j.MDC

class CustomLogger : Logger {
    companion object {
        private val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(CustomLogger::class.java)
    }

    override fun error(message: String, mdc: Map<String, String>, exception: Exception?) {
        MDC.setContextMap(mdc)
        log.error(message, exception)
        MDC.clear()
    }

    override fun warn(message: String, mdc: Map<String, String>, exception: Exception?) {
        MDC.setContextMap(mdc)
        log.warn(message, exception)
        MDC.clear()
    }

    override fun info(message: String, mdc: Map<String, String>, exception: Exception?) {
        MDC.setContextMap(mdc)
        log.info(message, exception)
        MDC.clear()
    }
}