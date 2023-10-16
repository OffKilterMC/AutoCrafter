package net.offkiltermc.autocrafter

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.io.FileNotFoundException
import java.lang.Exception
import java.nio.file.Path

class Config {
    var useDropperBehavior = false

    init {
        try {
            filePath.toFile().reader().use { reader ->
                val root = JsonParser.parseReader(reader)
                val obj = root.asJsonObject

                useDropperBehavior = obj.getAsJsonPrimitive("useDropperBehavior").getAsBoolean()
            }
        } catch (e: Exception) {
            if (e is FileNotFoundException) {
                filePath.toFile().writer().use { writer ->
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    gson.toJson(this, writer)
                }
            } else {
                throw (e)
            }
        }
    }

    companion object {
        private val filePath: Path by lazy {
            FabricLoader.getInstance().configDir.resolve("auto_crafter.json")
        }

        val INSTANCE: Config by lazy {
            Config()
        }
    }
}