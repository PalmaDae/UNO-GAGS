package client.common

import javafx.scene.image.Image
import proto.dto.CardColor

object ResourceLoader {
    private const val RESOURCE_BASE = "/images/cards/"

    private fun getCardImagePath(card: proto.dto.Card): String {
        val type = card.type
        val color = card.color

        if (type == proto.dto.CardType.BACK) return "${RESOURCE_BASE}back.png"

        val colorDir = if (type == proto.dto.CardType.WILD || type == proto.dto.CardType.WILD_DRAW_FOUR) {
            "wild"
        } else {
            color.name.lowercase()
        }

        val fileName = when (type) {
            proto.dto.CardType.NUMBER -> "${card.number}.png"
            proto.dto.CardType.DRAW_TWO -> "+2.png"
            proto.dto.CardType.REVERSE -> "reverse.png"
            proto.dto.CardType.SKIP -> "skip.png"
            proto.dto.CardType.WILD_DRAW_FOUR -> "+4.png"
            proto.dto.CardType.WILD -> "wildcard.png"
            else -> "${type.name.lowercase()}.png"
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

    fun loadCardImage(card: proto.dto.Card): Image {
        val path = getCardImagePath(card)
        println("DEBUG: Loading Card: ${card.color}_${card.type} (num: ${card.number}) -> Path: $path")

        return try {
            val stream = javaClass.getResourceAsStream(path)
            if (stream == null) {
                println("Error: Resource not found at path: $path")
                val backStream = javaClass.getResourceAsStream("${RESOURCE_BASE}back.png")
                if (backStream != null) Image(backStream) else Image("about:blank")
            } else {
                Image(stream)
            }
        } catch (e: Exception) {
            println("Error loading image $path: ${e.message}")
            Image("about:blank")
        }
    }
}