package dev.moru3.mapscreen

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MapScreen : JavaPlugin() {

    private val videoPath = this.dataFolder.resolve("video.mp4")

    override fun onEnable() {
        videoPath.takeUnless(File::exists)?.also { this.saveResource("video.mp4", false) }
    }

    override fun onDisable() {
        MultiThreadScheduler.timers.forEach(MultiThreadScheduler::stop)
    }
}