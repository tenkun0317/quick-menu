plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        string(current.version == "26.1") {
            replace("?.gui?.setScreen(", "?.setScreen(")
            replace(".gui.setScreen(", ".setScreen(")
            replace(".gui.screen()", ".screen")
        }
    }
}
