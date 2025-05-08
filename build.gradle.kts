import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
	id = "korlibs.korge.starterkit.platformer"
    name = "Korge Platformer"
    title = name
    icon = file("icon.png")

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	targetJs()
    //targetIos()
	targetAndroid()

	serializationJson()
}


dependencies {
    add("commonMainApi", project(":deps"))
    //add("commonMainApi", project(":korge-dragonbones"))
}

// @TODO: HACK won't be required after 6.0.0-beta5
tasks {
    val browserEsbuildResources by getting(Copy::class)
    val browserReleaseWebpack by getting(Copy::class)

    fun CopySpec.registerModulesResources(project: Project) {
        project.afterEvaluate {
            for (file in (project.rootDir.resolve("modules").listFiles()?.toList() ?: emptyList())) {
                from(File(file, "resources"))
                from(File(file, "src/commonMain/resources"))
            }
        }
    }

    browserEsbuildResources.registerModulesResources(project)
    browserReleaseWebpack.registerModulesResources(project)
}
