import com.dragonbones.core.AnimationFadeOutMode
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.dragonbones.KorgeDbArmatureDisplay
import korlibs.korge.dragonbones.KorgeDbFactory
import korlibs.korge.input.keys
import korlibs.korge.ldtk.view.readLDTKWorld
import korlibs.korge.mascots.KorgeMascotsAnimations
import korlibs.korge.mascots.buildArmatureDisplayGest
import korlibs.korge.mascots.loadKorgeMascots
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.math.isAlmostZero
import korlibs.time.TimeSpan
import korlibs.time.milliseconds
import korlibs.time.seconds
import kotlin.math.absoluteValue

suspend fun main() = Korge(
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
	override suspend fun SContainer.sceneMain() {
		val world = resourcesVfs["ldtk/Typical_2D_platformer_example.ldtk"].readLDTKWorld()
		val collisions = world.createCollisionMaps()
		//val mapView = LDTKViewExt(world, showCollisions = true)
		val mapView = LDTKViewExt(world, showCollisions = false)
		//println(collisions)
		val db = KorgeDbFactory()
        db.loadKorgeMascots()

        val player = db.buildArmatureDisplayGest()!!
			.xy(200, 200)
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
            """.trimIndent()
        ).xy(8, 8)

		val gravity = Vector2(0, 10.0)
		var playerSpeed = Vector2(0, 0)
        val mapBounds = mapView.getLocalBounds()

        var currentRect = Rectangle(0, 0, 1024, 1024)

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

		keys {
			fun updated(right: Boolean, up: Boolean) {
				if (!up) {
					player.scaleX = player.scaleX.absoluteValue * if (right) +1f else -1f
					tryMoveDelta(Point(2.0, 0) * (if (right) +1 else -1))
					player.speed = 2f
					moving = true
				} else {
					player.speed = 1f
					moving = false
				}
                updateState()
                //updateTextContainerPos()
			}
			up(Key.LEFT, Key.RIGHT) {
				updated(right = it.key == Key.RIGHT, up = true)
			}
			downFrame(Key.LEFT, dt = 16.milliseconds) {
				updated(right = false, up = false)
			}
			downFrame(Key.RIGHT, dt = 16.milliseconds) {
				updated(right = true, up = false)
			}
			down(Key.SPACE) {
				val isInGround = playerSpeed.y.isAlmostZero()
				//if (isInGround) {
				if (true) {
                    if (!jumping) {
                        jumping = true
                        updateState()
                    }
					playerSpeed += Vector2(0, -4)
				}
			}
		}

		addUpdater {
			playerSpeed += gravity * it.seconds
			if (!tryMoveDelta(playerSpeed)) {
				playerSpeed = Vector2.ZERO
                if (jumping) {
                    jumping = false
                    updateState()
                }
			}
		}

        currentRect = Rectangle.getRectWithAnchorClamped(player.pos, Size(32, 32), Anchor.CENTER, mapBounds)

        var zoom = Size(256, 256)

        keys {
            down(Key.Z) {
                val zoomC = zoom.avgComponent()
                val zoomC2 = if (zoomC >= 1024f) 128f else zoomC * 2
                zoom = Size(zoomC2, zoomC2)
            }
        }

        addUpdater {
            val newRect = Rectangle.getRectWithAnchorClamped(player.pos, zoom, Anchor.CENTER, mapBounds)
            currentRect = (0.05 * 0.5).toRatio().interpolate(currentRect, newRect)
            //camera.setTo(currentRect.rounded())
            camera.setTo(currentRect)
        }
	}
}

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


    if (rect.right > clampBounds.right || rect.bottom > clampBounds.bottom) {
        return rect.applyScaleMode(clampBounds, ScaleMode.SHOW_ALL, Anchor.TOP_LEFT)
    } else {
        return rect
    }
}
