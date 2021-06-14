package dev.moru3.mapscreen

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player

class MapRenderer(val screen: Screen, val itemFrame: ItemFrame, val location: Location, val position: Int, val mapId: Int, val frames: MutableList<List<Byte>> = mutableListOf()) {
    val version = Bukkit.getServer().javaClass.`package`.name.replace(".", ",").split(",")[3]
    private var frame = 0
    val x = position%screen.width
    val y = position/screen.width
    var scheduler: MultiThreadScheduler? = null

    private var oldData = listOf<Byte>()

    fun getNmsClass(className: String): Class<*> { return Class.forName("net.minecraft.server.${version}.${className}") }

    fun getCraftBukkitNmsClass(className: String): Class<*> { return Class.forName("org.bukkit.craftbukkit.${version}.${className}") }

    fun getNmsPlayer(player: Player): Any {
        return player.javaClass.getMethod("getHandle").invoke(player)
    }

    fun Player.sendPacket(packet: Any) {
        val nmsPlayer = getNmsPlayer(this)
        val con = nmsPlayer.javaClass.getField("playerConnection").get(nmsPlayer)
        val sendPacket = getNmsClass("PlayerConnection").getMethod("sendPacket", getNmsClass("Packet"))
        sendPacket.invoke(con, packet)
    }

    fun start() {
        Bukkit.broadcastMessage("スケジュールを開始しました。")
        scheduler = MultiThreadScheduler({
            rendering()
        }, (20/screen.frameRate).toLong(), false)
    }

    fun rendering() {
        Deferrable {
            defer { frame++ }
            if(frames.size<=frame) { frame = 0 }
            val frame = frames[frame]

            var startX = 127
            var startY = 127
            var endX = 0
            var endY = 0

            var match = true
            frame.forEachIndexed { index, byte ->
                if(oldData[index]!=byte) {
                    match = false
                    if(index%128<startX) { startX = index%128 }
                    if(index/128<startY) { startY = index/128 }
                    if(index%128>endX) { endX = index%128 }
                    if(index/128>endY) { endY = index/128 }
                }
            }
            match&&return@Deferrable

            val packet = getNmsClass("PacketPlayOutMap")
                .getConstructor(Int::class.java, Byte::class.java, Boolean::class.java, Boolean::class.java, Collection::class.java, ByteArray::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                .newInstance(mapId, 0.toByte(), true, false, listOf<Any>(), frame.toByteArray(), startX, startY, endX, endY)
            Bukkit.getOnlinePlayers().forEach {
                it.sendPacket(packet)
            }
        }
    }
}