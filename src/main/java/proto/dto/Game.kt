package proto.dto

import proto.common.Payload

data class GameState(
    val roomId: Long,
    val players: Map<Long, PlayerGameInfo>, // userId -> инфо
    val currentCard: Card?,
    val currentPlayerId: Long,
    val direction: GameDirection,
    val gamePhase: GamePhase
) : Payload

data class PlayerHandUpdate(
    val hand: List<Card>
) : Payload

data class PlayerGameInfo(
    val username: String,
    val cardCount: Int,
    val hasUno: Boolean = false
) : Payload

// направление: по часовой и против часовой
enum class GameDirection {
    CLOCKWISE, COUNTER_CLOCKWISE
}

data class ChooseColorRequest(
    val roomId: Long,
    val chosenColor: CardColor
) : Payload

enum class GamePhase {
    WAITING_TURN, CHOOSING_COLOR, DRAWING_CARD, FINISHED
}

data class PlayCardRequest(
    val roomId: Long,
    val cardIndex: Int,           // Индекс карты в руке
    val chosenColor: CardColor? = null // Для WILD карт
) : Payload

data class ChatMessage
@JvmOverloads
constructor(
    val senderId: Long,
    val senderName: String,
    val content: String,
    val messageType: ChatMessageType = ChatMessageType.TEXT,
    val emojiId: String? = null,  // Для смайлов
    val timestamp: Long = System.currentTimeMillis()
) : Payload

enum class ChatMessageType {
    TEXT, EMOJI, VOICE
}

// Карта (базовая модель)
data class Card(
    val id: String,
    val color: CardColor,
    val type: CardType,
    val number: Int? = null  // только для NUMBER
) : Payload

// карта: красная, синяя, зелёная, жёлтая, дикая
enum class CardColor { RED, BLUE, GREEN, YELLOW, WILD }
// карта: обычная, пропуск, разворот, +2, дикая, дикая+4
enum class CardType { NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR }