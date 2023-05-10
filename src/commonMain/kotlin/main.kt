import com.dragonbones.core.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.dragonbones.*
import korlibs.korge.input.*
import korlibs.korge.ldtk.view.*
import korlibs.korge.mascots.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.virtualcontroller.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.math.*

suspend fun main() = Korge(
    //windowSize = Size(1280, 720),
    windowSize = Size(512, 512),
    backgroundColor = Colors["#2b2b2b"],
    displayMode = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor.TOP_LEFT, clipBorders = false),
) {
    val sceneContainer = sceneContainer()



    sceneContainer.changeTo({ MyScene() })
}

object COLLISIONS {
    val OUTSIDE = -1
    val EMPTY = 0
    val DIRT = 1
    val LADDER = 2
    val STONE = 3

    fun isSolid(type: Int, direction: Vector2): Boolean {
        return type == DIRT || type == STONE || type == OUTSIDE
    }
}

class MyScene : Scene() {
    @KeepOnReload
    var currentPlayerPos = Point(200, 200)

    @KeepOnReload
    var initZoom = 32f

    @KeepOnReload
    var zoom = 256f

    override suspend fun SContainer.sceneMain() {
        onStageResized { width, height ->
            size = Size(views.actualVirtualWidth, views.actualVirtualHeight)
        }
        val world = resourcesVfs["ldtk/Typical_2D_platformer_example.ldtk"].readLDTKWorldExt()
        val collisions = world.createCollisionMaps()
        //val mapView = LDTKViewExt(world, showCollisions = true)
        val mapView = LDTKViewExt(world, showCollisions = false)
        //println(collisions)
        val db = KorgeDbFactory()
        db.loadKorgeMascots()

        val player = db.buildArmatureDisplayGest()!!
            .xy(currentPlayerPos)
            .play(KorgeMascotsAnimations.IDLE)
            .scale(0.080)

        val camera = camera {
            this += mapView
            this += player
        }

        text(
            """
                Use the arrow keys '<-' '->' to move Gest
                'z' for zoom
                Space for jumping
                startPos = $currentPlayerPos
            """.trimIndent()
        ).xy(8, 8)

        val textCoords = text("-").xy(8, 200)

        mapView.mouse {
            move {
                textCoords.text = collisions.pixelToTile(it.currentPosLocal.toInt()).toString()
            }
        }

        val buttonRadius = 110f
        val virtualController = virtualController(
            buttons = listOf(
                VirtualButtonConfig.SOUTH,
                VirtualButtonConfig(Key.Z, GameButton.START, Anchor.BOTTOM_RIGHT, offset = Point(0, -buttonRadius * 1.5f))
            ),
            buttonRadius = buttonRadius
        ).also { it.container.alpha(0.5f) }

        val gravity = Vector2(0, 10.0)
        var playerSpeed = Vector2(0, 0)
        val mapBounds = mapView.getLocalBounds()

        //var currentRect = Rectangle(0, 0, 1024, 1024)
        var currentCameraInfo = CameraInfo(Point.ZERO, 100f, Anchor.TOP_LEFT)

        fun tryMoveDelta(delta: Point): Boolean {
            val newPos = player.pos + delta

            val collisionPoints = listOf(
                newPos,
                newPos + Point(-5, 0),
                newPos + Point(+5, 0),
                newPos + Point(-5, -7),
                newPos + Point(+5, -7),
                newPos + Point(-5, -14),
                newPos + Point(+5, -14),
            )

            var set = collisionPoints.all { !COLLISIONS.isSolid(collisions.getPixel(it), delta) }
            if (set) {
                player.pos = newPos
                currentPlayerPos = newPos
            }
            return set
        }

        var playerState = "idle"
        fun setState(name: String, time: TimeSpan) {
            if (playerState != name) {
                playerState = name
                player.animation.fadeIn(playerState, time)
            }
        }

        var jumping = false
        var moving = false

        fun updateState() {
            when {
                jumping -> setState("jump", 0.1.seconds)
                moving -> setState("walk", 0.1.seconds)
                else -> setState("idle", 0.3.seconds)
            }
        }

        fun updated(right: Boolean, up: Boolean, scale: Float = 1f) {
            if (!up) {
                player.scaleX = player.scaleX.absoluteValue * if (right) +1f else -1f
                tryMoveDelta(Point(2.0, 0) * (if (right) +1 else -1) * scale)
                player.speed = 2f * scale
                moving = true
            } else {
                player.speed = 1f
                moving = false
            }
            updateState()
            //updateTextContainerPos()
        }

        virtualController.apply {
            down(GameButton.BUTTON_SOUTH) {
                val isInGround = playerSpeed.y.isAlmostZero()
                //if (isInGround) {
                if (true) {
                    if (!jumping) {
                        jumping = true
                        updateState()
                    }
                    playerSpeed += Vector2(0, -5.5)
                }
            }
            changed(GameButton.LX) {
                if (it.new.absoluteValue < 0.01f) {
                    updated(right = it.new > 0f, up = true, scale = 1f)
                }
            }
        }

        fun createSize(zoom: Float): Size {
            return Size(zoom * (width / height), zoom)
        }

        //currentRect = Rectangle.getRectWithAnchorClamped(player.pos, createSize(initZoom), Anchor.CENTER, mapBounds)
        currentCameraInfo = CameraInfo(player.pos, initZoom, Anchor.CENTER)

        virtualController.down(GameButton.START) {
            val zoomC = zoom
            val zoomC2 = if (zoomC >= 1024f) 128f else zoomC * 2
            zoom = zoomC2
        }

        val FREQ = 60.hz
        addFixedUpdater(FREQ) {
            // Move character
            run {
                val lx = virtualController.lx.withoutDeadRange()
                when {
                    lx < 0f -> {
                        updated(right = false, up = false, scale = lx.absoluteValue)
                    }

                    lx > 0f -> {
                        updated(right = true, up = false, scale = lx.absoluteValue)
                    }
                }
            }

            // Apply gravity
            run {
                playerSpeed += gravity * FREQ.timeSpan.seconds
                if (!tryMoveDelta(playerSpeed)) {
                    playerSpeed = Vector2.ZERO
                    if (jumping) {
                        jumping = false
                        updateState()
                    }
                }
            }

            // Update camera
            run {
                val newCameraInfo = CameraInfo(player.pos, zoom, Anchor.CENTER)
                currentCameraInfo = (0.05 * 0.5).toRatio().interpolate(currentCameraInfo, newCameraInfo)
                //camera.setTo(currentRect.rounded())
                camera.setTo(Rectangle.getRectWithAnchorClamped(player.pos, createSize(zoom), Anchor.CENTER, mapBounds))
                initZoom = zoom
            }
        }
    }
}

data class CameraInfo(
    val pos: Point,
    val zoom: Float,
    val anchor: Anchor,
) : Interpolable<CameraInfo> {
    override fun interpolateWith(ratio: Ratio, other: CameraInfo): CameraInfo {
        return CameraInfo(
            ratio.interpolate(pos, other.pos),
            ratio.interpolate(zoom, other.zoom),
            ratio.interpolate(anchor, other.anchor),
        )
    }
}

// @TODO: Required for JS
fun Float.withoutDeadRange(): Float = if (this.absoluteValue >= 0.15f) this else 0f

fun KorgeDbArmatureDisplay.play(animationName: String): KorgeDbArmatureDisplay {
    animation.play(animationName)
    return this
}

fun KorgeDbArmatureDisplay.fadeIn(
    animationName: String, fadeInTime: TimeSpan, playTimes: Int = -1,
    layer: Int = 0, group: String? = null, fadeOutMode: AnimationFadeOutMode = AnimationFadeOutMode.SameLayerAndGroup
): KorgeDbArmatureDisplay? {
    animation.fadeIn(animationName, fadeInTime, playTimes, layer, group, fadeOutMode)
    return this
}

fun Rectangle.Companion.interpolated(a: Rectangle, b: Rectangle, ratio: Ratio): Rectangle = Rectangle.fromBounds(
    ratio.interpolate(a.left, b.left),
    ratio.interpolate(a.top, b.top),
    ratio.interpolate(a.right, b.right),
    ratio.interpolate(a.bottom, b.bottom),
)

fun Ratio.interpolate(l: Rectangle, r: Rectangle): Rectangle = Rectangle.interpolated(l, r, this)

fun Rectangle.Companion.getRectWithAnchor(pos: Point, size: Size, anchor: Anchor = Anchor.CENTER): Rectangle {
    val topLeft = pos - size * anchor
    return Rectangle(topLeft, size)
}

fun Rectangle.Companion.getRectWithAnchorClamped(pos: Point, size: Size, anchor: Anchor = Anchor.CENTER, clampBounds: Rectangle, keepProportions: Boolean = true): Rectangle {
    var rect = getRectWithAnchor(pos, size, anchor)
    if (rect.right > clampBounds.right) rect = rect.copy(x = rect.x - (rect.right - clampBounds.right))
    if (rect.bottom > clampBounds.bottom) rect = rect.copy(y = rect.y - (rect.bottom - clampBounds.bottom))
    if (rect.left < clampBounds.left) rect = rect.copy(x = rect.x - (rect.left - clampBounds.left))
    if (rect.top < clampBounds.top) rect = rect.copy(y = rect.y - (rect.top - clampBounds.top))


    //if (rect.right > clampBounds.right || rect.bottom > clampBounds.bottom) {
    //    return rect.applyScaleMode(clampBounds, ScaleMode.SHOW_ALL, Anchor.TOP_LEFT)
    //} else {
        return rect
    //}
}
