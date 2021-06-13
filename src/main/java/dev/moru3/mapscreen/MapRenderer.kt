package dev.moru3.mapscreen

import org.bukkit.Location
import org.bukkit.entity.ItemFrame

class MapRenderer(val itemFrame: ItemFrame, val location: Location, val position: Int, val mapId: Int, val frames: MutableList<Byte> = mutableListOf()) {
}