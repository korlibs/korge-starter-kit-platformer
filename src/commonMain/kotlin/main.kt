import korlibs.image.color.Colors
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.Korge
import korlibs.korge.dragonbones.KorgeDbArmatureDisplay
import korlibs.korge.dragonbones.KorgeDbFactory
import korlibs.korge.ldtk.view.LDTKView
import korlibs.korge.ldtk.view.readLDTKWorld
import korlibs.korge.mascots.KorgeMascotsAnimations
import korlibs.korge.mascots.buildArmatureDisplayGest
import korlibs.korge.mascots.loadKorgeMascots
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.scale
import korlibs.korge.view.xy
import korlibs.math.geom.Size

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo({ MyScene() })
}

class MyScene : Scene() {
	override suspend fun SContainer.sceneMain() {
		val world = resourcesVfs["ldtk/Typical_2D_platformer_example.ldtk"].readLDTKWorld()
		this += LDTKViewExt(world)
		val db = KorgeDbFactory()
		db.loadKorgeMascots()
		this += db.buildArmatureDisplayGest()!!
			.xy(200, 480)
			.play(KorgeMascotsAnimations.IDLE)
			.scale(0.5)
	}
}

fun KorgeDbArmatureDisplay.play(animationName: String): KorgeDbArmatureDisplay {
	animation.play(animationName)
	return this
}
