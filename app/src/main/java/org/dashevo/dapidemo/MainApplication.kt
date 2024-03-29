package org.dashevo.dapidemo

import android.app.Application
import android.content.Context
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import org.dashevo.dapiclient.DapiClient
import org.dashj.dashjinterface.WalletAppKitService
import org.dashj.dashjinterface.config.DevNetMaithainConfig
import org.slf4j.LoggerFactory
import java.io.File

class MainApplication : Application() {

    private val prefs by lazy { getSharedPreferences("dapi-demo", Context.MODE_PRIVATE) }
    val dapiClient by lazy { DapiClient("http://devnet-maithai.thephez.com", "3000") }

    companion object {
        lateinit var instance: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        initLogging()
        WalletAppKitService.init(this, DevNetMaithainConfig.get())
        instance = this
    }

    private fun initLogging() {
        val logDir = getDir("log", Context.MODE_PRIVATE)

        val logFile = File(logDir, "wallet.log")

        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val filePattern = PatternLayoutEncoder()
        filePattern.context = context
        filePattern.pattern = "%d{HH:mm:ss,UTC} [%thread] %logger{0} - %msg%n"
        filePattern.start()

        val fileAppender = RollingFileAppender<ILoggingEvent>()
        fileAppender.context = context
        fileAppender.file = logFile.getAbsolutePath()

        val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
            setContext(context)
            setParent(fileAppender)
        }
        rollingPolicy.fileNamePattern = logDir.absolutePath + "/wallet.%d{yyyy-MM-dd,UTC}.log.gz"
        rollingPolicy.maxHistory = 7
        rollingPolicy.start()

        fileAppender.encoder = filePattern
        fileAppender.rollingPolicy = rollingPolicy
        fileAppender.start()

        val logcatTagPattern = PatternLayoutEncoder()
        logcatTagPattern.context = context
        logcatTagPattern.pattern = "%logger{0}"
        logcatTagPattern.start()

        val logcatPattern = PatternLayoutEncoder()
        logcatPattern.context = context
        logcatPattern.pattern = "[%thread] %msg%n"
        logcatPattern.start()

        val logcatAppender = LogcatAppender()
        logcatAppender.context = context
        logcatAppender.tagEncoder = logcatTagPattern
        logcatAppender.encoder = logcatPattern
        logcatAppender.start()

        val log = context.getLogger(Logger.ROOT_LOGGER_NAME)
        log.addAppender(fileAppender)
        log.addAppender(logcatAppender)
        log.level = Level.INFO
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

}
