package dev.moru3.mapscreen

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) { return true }
        Bukkit.broadcastMessage((sender.location.yaw.toInt().run { if(this<0) this+360 else this }).toString())
        return super.onCommand(sender, command, label, args)
    }
}