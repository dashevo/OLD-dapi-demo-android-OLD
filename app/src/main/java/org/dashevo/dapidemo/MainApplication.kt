package org.dashevo.dapidemo

import android.app.Application
import org.dashj.dashjinterface.WalletAppKitService
import org.dashj.dashjinterface.config.DevNetMaithainConfig

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
        WalletAppKitService.init(this, DevNetMaithainConfig.get())
    }

    private fun initLogging() {
        /*val logDir = getDir("log", Context.MODE_PRIVATE)

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
        log.level = Level.INFO*/
    }
}
