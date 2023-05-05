import com.dragonbones.core.AnimationFadeOutMode
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.Korge
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
import korlibs.math.geom.Point
import korlibs.math.geom.Rectangle
import korlibs.math.geom.Size
import korlibs.math.geom.Vector2
import korlibs.math.isAlmostZero
import korlibs.time.TimeSpan
import korlibs.time.milliseconds
import korlibs.time.seconds
import kotlin.math.absoluteValue

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
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
		val SCALE = 1.5
		scale(SCALE)
		//xy(0, 200)

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

		val gravity = Vector2(0, 10.0)
		var playerSpeed = Vector2(0, 0)
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

			if (collisionPoints.all { !COLLISIONS.isSolid(collisions.getPixel(it), delta) }) {
				player.pos = newPos
				camera.setTo(Rectangle((player.pos * SCALE - Vector2(200, 200)), Size(400, 400) * SCALE))
				return true
			} else {
				return false
			}
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
