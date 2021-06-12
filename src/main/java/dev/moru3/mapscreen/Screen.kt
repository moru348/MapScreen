package dev.moru3.mapscreen

import org.bukkit.Location
import org.bukkit.map.MapPalette
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.*

class Screen(private val file: File, val pos1: Location, val pos2: Location) {

    private val height: Int
    private val width: Int
    private val baseDirection: DirectionType

    private val frames = mutableListOf<List<Byte>>()
    private var frame = 0
    private var base: BaseType

    private var scheduler: MultiThreadScheduler? = null
    private val frameRate = 20

    private val mag: Int

    fun BufferedImage.resize(newWidth: Int, newHeight: Int): BufferedImage {
        return BufferedImage(newWidth, newHeight, this.type).apply {
            createGraphics().also {
                it.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                it.drawImage(this, 0, 0, newWidth, newHeight, 0, 0, this.width, this.height, null)
                it.dispose()
            } }
    }

    fun Int.aMinus(integer: Int): Int { return max(this, integer)-min(this, integer) }

    init {
        file.takeUnless(File::exists)?.also { throw NullPointerException("video.mp4が存在しません。(${file.absolutePath})") }
        if(pos1.world!=pos2.world) { throw IllegalArgumentException("pos1とpos2は同じワールドである必要があります。") }
        if(listOf(pos1.blockX-pos2.blockX,pos1.blockZ-pos2.blockZ,pos1.blockY-pos2.blockY)
                .map(0::equals).filter(true::equals).size!=2) { throw IllegalArgumentException("abはx,yのどちらかが0になる必要があるヨ！") }
        when {
            pos1.blockX-pos2.blockX==0 -> {
                baseDirection = DirectionType.X
                height = pos1.blockX.aMinus(pos2.blockX)
            }
            pos1.blockZ-pos2.blockZ==0 -> {
                baseDirection = DirectionType.Z
                height = pos1.blockZ.aMinus(pos2.blockZ)
            }
            else -> {
                baseDirection = DirectionType.Y

            }
        }
        width = pos1.blockY.aMinus(pos2.blockY)

        val frameGrabber = FFmpegFrameGrabber(file)
        if(frameGrabber.frameRate<20) { throw IllegalArgumentException("動画ファイルのフレームレートは20以上である必要があります。") }
        if(width<0) { throw IllegalArgumentException("widthは0以上にする必要があります。") }
        if(height<0) { throw IllegalArgumentException("heightは0以上にする必要があります。") }

        base = BaseType.WIDTH
        mag = ((width*128)/frameGrabber.imageWidth).takeUnless { frameGrabber.imageHeight*it>height*128 }?: run {
            base=BaseType.HEIGHT
            (width*128)/frameGrabber.imageHeight
        }
        val height = frameGrabber.imageHeight*mag
        val width = frameGrabber.imageWidth*mag

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

enum class BaseType {
    WIDTH,
    HEIGHT
}

enum class DirectionType { X, Y, Z }