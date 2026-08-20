pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "minecraft-plugins"

include(":plugins:fancynpcs-v2:")
include(":plugins:fancynpcs-v2:fn-v2-api")
include(":plugins:fancynpcs-v2:implementation_1_21_6")

include(":plugins:fancyholograms-v2")
include(":plugins:fancyholograms-v2:api")

include(":plugins:fancydialogs")
include(":plugins:fancydialogs:fd-api")

include(":libraries:common")
include(":libraries:jdb")
include(":libraries:config")
include(":libraries:plugin-tests")

include(":libraries:packets")
include(":libraries:packets:packets-api")
include(":libraries:packets:implementations:1_21_6")
