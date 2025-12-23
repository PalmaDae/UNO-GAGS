package server.game

import proto.dto.Card
import server.common.Connection

class PlayerState(
    val id: Long,
    val username: String,
    val avatar: String = "default.png",
    val connection: Connection
) {
    var hasDeclaredUno = false
        private set
    var hand: MutableList<Card> = ArrayList()
        private set

    val cardCount: Int
        get() = hand.size

    fun addCard(card: Card?) {
        card?.let {
            if (hand.size > 2)
                hasDeclaredUno = false
            hand.add(it)
        }
    }

    fun removeCard(cardIndex: Int): Card? {
        require(!(cardIndex < 0 || cardIndex >= hand.size)) { "Invalid card index: $cardIndex" }
        if (hand.size > 2)
            hasDeclaredUno = false
        val removedCard: Card? = hand.removeAt(cardIndex)
        return removedCard
    }

    fun declareUno() {
        if (hand.size == 2)
            hasDeclaredUno = true
        else
            throw IllegalStateException("Can only declare UNO with exactly 2 cards")
    }
}