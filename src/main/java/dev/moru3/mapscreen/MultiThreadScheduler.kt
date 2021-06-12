package dev.moru3.mapscreen

import java.util.*

class MultiThreadScheduler(var runnable: (MultiThreadScheduler)->Unit, tick: Long, val ignoreException: Boolean = true, val endRunnable: ()->Unit = {}) {

    companion object {
        val timers = mutableListOf<MultiThreadScheduler>()
    }

    private val timer = Timer()
    var isCanceled = false
        private set

    fun stop() { isCanceled = true }


    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            private var thread: Thread? = null
            override fun cancel(): Boolean {
                endRunnable.invoke()
                thread?.interrupt()
                return super.cancel()
            }
            override fun run() {
                thread = Thread.currentThread()
                if(isCanceled) {
                    cancel()
                } else {
                    try {
                        runnable.invoke(this@MultiThreadScheduler)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if(!ignoreException) {
                            stop()
                        }
                    }
                }
            }
        }, 0, tick*50)
        timers.add(this)
    }
}

class MultiThreadRunner(runnable: () -> Unit) {
    init {
        Thread {
            runnable.invoke()
            Thread.currentThread().interrupt()
        }.start()
    }
}