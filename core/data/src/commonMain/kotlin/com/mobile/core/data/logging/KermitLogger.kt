package com.mobile.core.data.logging

import co.touchlab.kermit.Logger
import com.mobile.core.domain.logging.GoZipoLogger

object KermitLogger : GoZipoLogger {

    override fun debug(message: String) {
        Logger.d(message)
    }

    override fun info(message: String) {
        Logger.i(message)
    }

    override fun warn(message: String) {
        Logger.w(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        Logger.e(message, throwable)
    }
}