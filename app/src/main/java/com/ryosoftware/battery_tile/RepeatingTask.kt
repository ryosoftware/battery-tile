package com.ryosoftware.battery_tile

import android.os.Handler

class RepeatingTask(
    private val handler: Handler,
    private val tag: String,
    private val logger: Logger,
    private val task: RepeatingTask.() -> Unit
) {
    private var postedRunnable: Runnable? = null

    var iteration = 0
        private set

    fun startRepeating(initialDelayMillis: Long, repeatsDelayMillis: Long) {
        stop()

        val runnable = object : Runnable {
            override fun run() {
                logger.log("$tag running now (iteration $iteration)")

                task()
                iteration++
                if (repeatsDelayMillis != 0L) {
                    logger.log("$tag will rerun in ${repeatsDelayMillis / 1000} seconds, at $TIME_REFERENCE", System.currentTimeMillis() + repeatsDelayMillis)

                    handler.postDelayed(this, repeatsDelayMillis)
                }
            }
        }

        logger.log("$tag will be executed in ${initialDelayMillis / 1000} seconds, at $TIME_REFERENCE", System.currentTimeMillis() + repeatsDelayMillis)

        iteration = 0
        postedRunnable = runnable
        if (initialDelayMillis > 0) {
            handler.postDelayed(runnable, initialDelayMillis)
        } else {
            handler.post(runnable)
        }
    }

    fun startRepeating(delay: Long) = startRepeating(delay, delay)

    fun executeDelayed(initialDelayMillis: Long) = startRepeating(initialDelayMillis, 0L)

    fun executeNow() {
        logger.log("$tag running forced now")

        task()
    }

    fun stop() {
        if ((BuildConfig.DEBUG) && (postedRunnable != null)) logger.log("$tag pending tasks removed")

        postedRunnable?.let { handler.removeCallbacks(it) }
        postedRunnable = null
    }
}