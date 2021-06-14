package dev.moru3.mapscreen.utils

import dev.moru3.mapscreen.MapScreen
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.jetbrains.annotations.NotNull

class Position(private val main: MapScreen): Listener {
    private val position1 = mutableMapOf<Player, Location>()
    private val position2 = mutableMapOf<Player, Location>()
    private val position = mutableMapOf<Player, Location>()
    private val multiplePositions = mutableMapOf<Player, MutableList<Location>>()
    private val positionMode = mutableMapOf<Player, PositionMode>()

    fun getRange(player: Player): Pair<Location, Location>? {
        if(positionMode[player]?: PositionMode.RANGE != PositionMode.RANGE) { return null }
        setMode(player, PositionMode.RANGE)
        return Pair(position1[player]?:return null, position2[player]?:return null).also { position1.remove(player); position2.remove(player) }
    }

    fun getPosition(player: Player): Location? {
        if(positionMode[player]!= PositionMode.ONE_POINT) { return null }
        setMode(player, PositionMode.RANGE)
        return (position[player]?:return null).also { position.remove(player) }
    }

    fun getMultiplePosition(player: Player): MutableList<Location>? {
        if(positionMode[player]!= PositionMode.MULTIPLE) { return null }
        setMode(player, PositionMode.RANGE)
        return (multiplePositions[player]?:return null).also { multiplePositions.remove(player) }
    }

    @NotNull
    fun setMode(player: Player, mode: PositionMode) { positionMode[player] = mode }

    @EventHandler
    private fun onClick(event: PlayerInteractEvent) {
        if(event.hand == EquipmentSlot.OFF_HAND) { return }
        if(event.player.inventory.itemInMainHand.type!= Material.DIAMOND_HORSE_ARMOR) { return }
        event.isCancelled = true
        val location = event.clickedBlock?.location?:return
        when(positionMode[event.player]?: PositionMode.RANGE) {
            PositionMode.RANGE -> {
                //範囲
                if(event.action==Action.LEFT_CLICK_BLOCK) {
                    if(position1[event.player]==location) { return }
                    position1[event.player] = location
                    event.player.sendMessage("set_position_1")
                } else if(event.action==Action.RIGHT_CLICK_BLOCK) {
                    if(position2[event.player]==location) { return }
                    position2[event.player] = location
                    event.player.sendMessage("set_position_2")
                }
            }
            PositionMode.MULTIPLE -> {
                //複数
                if(event.action==Action.LEFT_CLICK_BLOCK) {
                    if(multiplePositions[event.player]?.contains(location) == false) { return }
                    multiplePositions[event.player] = (multiplePositions[event.player]?: mutableListOf()).apply { remove(location) }
                    event.player.sendMessage("delete_position")
                } else if(event.action==Action.RIGHT_CLICK_BLOCK) {
                    if(multiplePositions[event.player]?.contains(location) == true) { return }
                    multiplePositions[event.player] = (multiplePositions[event.player]?:mutableListOf()).apply { add(location) }
                    event.player.sendMessage("add_position")
                }
            }
            PositionMode.ONE_POINT -> {
                if(event.action!=Action.RIGHT_CLICK_BLOCK) { return }
                if(position[event.player]==location) { return }
                position[event.player] = location
                event.player.sendMessage("set_position")
            }
        }
    }
}