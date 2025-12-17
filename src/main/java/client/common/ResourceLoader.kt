package client.common

import javafx.scene.image.Image
import proto.dto.CardColor

object ResourceLoader {
    private const val RESOURCE_BASE = "/images/cards/"

    private fun getCardImagePath(color: String, value: String): String {
        if (value == "BACK") {
            return "${RESOURCE_BASE}back.png"
        }

        val colorDir = if (color == "WILD" || color == "NONE") "wild" else color.lowercase()

        val fileName = when (value) {
            "PLUS_TWO" -> "+2.png"
            "REVERSE" -> "reverse.png"
            "SKIP" -> "skip.png"
            "WILD_DRAW_FOUR" -> "+4.png"
            "WILD" -> "wildcard.png"
            else -> "$value.png"
        }

        return "$RESOURCE_BASE$colorDir/$fileName"
    }

    fun toCssColor(color: CardColor): String {
        return when (color) {
            CardColor.RED -> "red"
            CardColor.BLUE -> "blue"
            CardColor.GREEN -> "green"
            CardColor.YELLOW -> "yellow"
            CardColor.WILD -> "gray"
        }
    }

    fun loadAvatar(avatarName: String): Image {
        val path = "/images/avatars/$avatarName"
        val resource = javaClass.getResource(path)
        return if (resource != null) {
            Image(resource.toExternalForm())
        } else {
            val defaultRes = javaClass.getResource("/images/avatars/default.png")
            if (defaultRes != null) {
                Image(defaultRes.toExternalForm())
            } else {
                Image("about:blank", 60.0, 60.0, true, true)
            }
        }
    }

    fun loadCardImage(color: String, value: String): Image {
        val path = getCardImagePath(color, value)
        return try {
            val stream = javaClass.getResourceAsStream(path)
            if (stream == null) {
                println("Error: Resource not found at path: $path")
                Image(javaClass.getResourceAsStream("/images/cards/back.png"))
            } else {
                Image(stream)
            }
        } catch (e: Exception) {
            println("Error loading image $path: ${e.message}")
            Image(javaClass.getResourceAsStream("/images/cards/back.png"))
        }
    }
}