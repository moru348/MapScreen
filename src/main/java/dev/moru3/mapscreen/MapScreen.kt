package dev.moru3.mapscreen

import dev.moru3.mapscreen.utils.Position
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MapScreen : JavaPlugin() {

    private val videoPath = this.dataFolder.resolve("video.mp4")
    private val position = Position(this)

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(position, this)
        videoPath.takeUnless(File::exists)?.also { this.saveResource("video.mp4", false) }
    }

    override fun onDisable() {
        MultiThreadScheduler.timers.forEach(MultiThreadScheduler::stop)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) { return true }
        try {
            val pos = position.getRange(sender)?:return true
            Screen(10, dataFolder.resolve("video.mp4"), pos.first, pos.second, Direction.EAST)
        } catch (e: Exception) { sender.sendMessage(e.message?:"") }
        return super.onCommand(sender, command, label, args)
    }
}