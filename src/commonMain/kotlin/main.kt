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
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.filter.IdentityFilter
import korlibs.korge.view.filter.filters
import korlibs.korge.view.scale
import korlibs.korge.view.xy
import korlibs.math.geom.*

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo({ MyScene() })
}

class MyScene : Scene() {
	override suspend fun SContainer.sceneMain() {
		scale(1.5)
		//xy(0, 200)

		val world = resourcesVfs["ldtk/Typical_2D_platformer_example.ldtk"].readLDTKWorld()
		val collisions = world.createCollisionMaps()
		val mapView = LDTKViewExt(world).filters(IdentityFilter)
		this += mapView
		mapView.mouse {
			onMove {
				//println("tilePos: ${it.currentPosLocal} : ${collisions.getPixel(it.currentPosLocal.toInt())}")
			}
		}
		//println(collisions)
		val db = KorgeDbFactory()
		db.loadKorgeMascots()

		val player = db.buildArmatureDisplayGest()!!
			.xy(200, 200)
			.play(KorgeMascotsAnimations.IDLE)
			.scale(0.125)

		this += player

		val gravity = Vector2(0, 10.0)
		var playerSpeed = Vector2(0, 0)
		fun tryMoveDelta(delta: Point): Boolean {
			val newPos = player.pos + delta
			if (collisions.getPixel(newPos) == 0) {
				player.pos = newPos
				return true
			} else {
				return false
			}
		}

		keys {
			downFrame(Key.LEFT) { tryMoveDelta(Point(-1, 0)) }
			downFrame(Key.RIGHT) { tryMoveDelta(Point(+1, 0)) }
			down(Key.SPACE) {
				playerSpeed += Vector2(0, -5)
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
