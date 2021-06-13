package dev.moru3.mapscreen

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapPalette
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.*

class Screen(private val frameRate: Int = 10, private val file: File, tempPos1: Location, tempPos2: Location, val direction: Direction) {

    private val height: Int
    private val width: Int

    private val pos1: Location = tempPos1.clone()
    private val pos2: Location = tempPos2.clone()

    private val itemFrames = mutableListOf<MapRenderer>()

    private val frames = mutableListOf<List<Byte>>()
    private var frame = 0

    private var scheduler: MultiThreadScheduler? = null

    private val mag: Int

    private fun BufferedImage.resize(newWidth: Int, newHeight: Int): BufferedImage {
        return BufferedImage(newWidth, newHeight, this.type).apply {
            createGraphics().also {
                it.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                it.drawImage(this, 0, 0, newWidth, newHeight, 0, 0, this.width, this.height, null)
                it.dispose()
            } }
    }

    private fun angleDistance(a: Int, b: Int): Int {
        return (abs(b - a) % 360).run { if(this>180) this - 360 else this }
    }

    init {
        if(!Bukkit.isPrimaryThread()) { throw Exception("ScreenはBukkitのPrimaryThreadから呼び出してください。") }
        if(frameRate !in 1..20) { throw IllegalArgumentException("フレームレートは1以上かつ20以下である必要があります。") }
        this.pos1.also { it.x=min(tempPos1.x, tempPos2.x);it.y=min(tempPos1.y, tempPos2.y);it.z=min(tempPos1.z, tempPos2.z) }
        this.pos2.also { it.x=max(tempPos1.x, tempPos2.x);it.y=max(tempPos1.y, tempPos2.y);it.z=max(tempPos1.z, tempPos2.z) }
        file.takeIf(File::exists)?:throw NullPointerException("video.mp4が存在しません。(${file.absolutePath})")
        if(pos1.world!=pos2.world) { throw IllegalArgumentException("pos1とpos2は同じワールドである必要があります。") }
        if(listOf(pos1.blockX-pos2.blockX,pos1.blockZ-pos2.blockZ,pos1.blockY-pos2.blockY)
                .map(0::equals).filter(true::equals).size!=2) { throw IllegalArgumentException("abはx,yのどちらかが0になる必要があるヨ！") }
        when {
            pos2.blockX-pos1.blockX==0 -> {
                if(direction !in listOf(Direction.NORTH, Direction.SOUTH)) {
                    throw IllegalArgumentException("pos1 pos2とdirectionが合っていません。")
                }
                height = pos2.blockZ - pos1.blockZ
                width = pos2.blockY - pos1.blockY
            }
            pos2.blockZ-pos1.blockZ==0 -> {
                if(direction !in listOf(Direction.EAST, Direction.WEST)) {
                    throw IllegalArgumentException("pos1 pos2とdirectionが合っていません。")
                }
                height = pos2.blockX - pos1.blockX
                width = pos2.blockY - pos1.blockY
            }
            else -> {
                if(direction !in listOf(Direction.BOTTOM, Direction.TOP)) {
                    throw IllegalArgumentException("pos1 pos2とdirectionが合っていません。")
                }
                height = pos2.blockX - pos1.blockX
                width = pos2.blockZ - pos1.blockZ
            }
        }

        val frameGrabber = FFmpegFrameGrabber(file)
        if(frameGrabber.frameRate<20) { throw IllegalArgumentException("動画ファイルのフレームレートは20以上である必要があります。") }
        if(width<0) { throw IllegalArgumentException("widthは0以上にする必要があります。") }
        if(height<0) { throw IllegalArgumentException("heightは0以上にする必要があります。") }

        mag = ((width*128)/frameGrabber.imageWidth).takeUnless { frameGrabber.imageHeight*it>height*128 }?:(width*128)/frameGrabber.imageHeight
        val height = frameGrabber.imageHeight*mag
        val width = frameGrabber.imageWidth*mag
        val world = pos1.world?:throw IllegalArgumentException("worldがnullです。")

        for(x in pos1.blockX..pos2.blockX) {
            for(y in pos1.blockY..pos2.blockY) {
                for(z in pos1.blockZ..pos2.blockZ) {
                    val itemFrame = world.spawnEntity(pos1.clone()
                        .also { it.x=x.toDouble();it.y=y.toDouble();it.z=z.toDouble() }, EntityType.ITEM_FRAME) as ItemFrame
                    itemFrame.isInvulnerable = true
                    itemFrame.itemDropChance = 0F
                    val location: Int
                    when(direction.type) {
                        1.toByte() -> {
                            //x y
                            location = y*width+x
                        }
                        2.toByte() -> {
                            //z y
                            location = y*width+z
                        }
                        3.toByte() -> {
                            //x z
                            throw IllegalArgumentException("現在上下はサポートしていません。")
                        }
                        else -> {
                            throw IllegalArgumentException("想定外のエラーが発生しました。")
                        }
                    }
                    var id: Int? = null
                    val map = ItemStack(Material.MAP).apply {
                        itemMeta = itemMeta?.also { mapMeta ->
                            if(mapMeta !is MapMeta) return@apply
                            mapMeta.mapView?.also { mapView ->
                                id = mapView.id
                                mapView.isLocked = true
                                mapView.renderers.forEach(mapView::removeRenderer)
                            }
                        }
                    }
                    itemFrame.setItem(map, false)
                    itemFrames.add(
                        MapRenderer(itemFrame, pos1.clone()
                            .also { it.x=x.toDouble();it.y=y.toDouble();it.z=z.toDouble() },
                            location, id?:throw Exception("予知せぬエラーが発生しました。"))
                    )
                }
            }
        }

        MultiThreadRunner {
            Java2DFrameConverter().also {
                val skipRate: Int = (frameGrabber.frameRate/frameRate).toInt()
                var count = 0
                while(frameGrabber.frameNumber<frameGrabber.lengthInFrames) {
                    if(frameGrabber.frameNumber%skipRate!=0) { continue }
                    val img: BufferedImage = it.convert(frameGrabber.grab())?.resize(width, height)?:continue
                    val list = mutableListOf<Byte>()
                    for(x in 0 until width) { for(y in 0 until height) { list += MapPalette.matchColor(img.getRGB(x, y).let { java.awt.Color(it and 0x00ff0000 shr 16, it and 0x0000ff00 shr 8, it and 0x000000ff) }) } }
                    frames.add(list)
                    count++
                }

                frameGrabber.stop()
                frameGrabber.close()
            }

            scheduler = MultiThreadScheduler({
                if(frame<=frames.size) { frame = 0;return@MultiThreadScheduler }



                frame++
            }, (20/frameRate).toLong(), false)
        }
        // }が続くのが気持ち悪かったのでコメント挟んどきます。
    }
}

enum class Direction(val type: Byte) { EAST(1), WEST(1), NORTH(2), SOUTH(2), TOP(3), BOTTOM(3) }