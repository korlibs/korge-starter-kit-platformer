import com.dragonbones.animation.AnimationState
import com.dragonbones.core.AnimationFadeOutMode
import korlibs.datastructure.IStackedIntArray2
import korlibs.datastructure.StackedIntArray2
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.Korge
import korlibs.korge.dragonbones.KorgeDbArmatureDisplay
import korlibs.korge.dragonbones.KorgeDbFactory
import korlibs.korge.input.keys
import korlibs.korge.input.mouse
import korlibs.korge.ldtk.view.LDTKView
import korlibs.korge.ldtk.view.readLDTKWorld
import korlibs.korge.mascots.KorgeMascotsAnimations
import korlibs.korge.mascots.buildArmatureDisplayGest
import korlibs.korge.mascots.loadKorgeMascots
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.*
import korlibs.korge.view.filter.IdentityFilter
import korlibs.korge.view.filter.filters
import korlibs.math.geom.*
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

	fun isSolid(type: Int): Boolean {
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
			.scale(0.125)

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
				newPos + Point(-5, -16),
				newPos + Point(+5, -16),
				newPos + Point(-5, -32),
				newPos + Point(+5, -32),
			)


			if (collisionPoints.all { !COLLISIONS.isSolid(collisions.getPixel(it)) }) {
				player.pos = newPos
				camera.setTo(Rectangle((player.pos - Vector2(200, 200)) * SCALE, Size(800, 800)))
				return true
			} else {
				return false
			}
		}

		keys {
			var moving = false
			fun updated(right: Boolean, up: Boolean) {
				if (!up) {
					player.scaleX = player.scaleX.absoluteValue * if (right) +1f else -1f
					tryMoveDelta(Point(2.0, 0) * (if (right) +1 else -1))
					if (!moving) player.animation.fadeIn(KorgeMascotsAnimations.WALK, 0.3.seconds)
					player.speed = 2f
					moving = true
				} else {
					player.speed = 1f
					if (moving) player.animation.fadeIn(KorgeMascotsAnimations.IDLE, 0.3.seconds)
					moving = false
				}
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
				playerSpeed += Vector2(0, -4)
			}
		}

		addUpdater {
			playerSpeed += gravity * it.seconds
			if (!tryMoveDelta(playerSpeed)) {
				playerSpeed = Vector2.ZERO
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
