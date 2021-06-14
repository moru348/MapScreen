package dev.moru3.mapscreen

class Deferrable(runnable: Deferrable.() -> Unit) {

    private val deferActions = mutableListOf<()->Unit>()

    fun defer(runnable: () -> Unit) {
        deferActions.add(runnable)
    }

    init {
        try {
            runnable.invoke(this)
            try { deferActions.forEach{ it.invoke() } } catch (e: Exception) { e.printStackTrace() }
        } catch (e: Exception) {
            try { deferActions.forEach{ it.invoke() } } catch (e: Exception) { e.printStackTrace() }
            throw e
        }
    }
}