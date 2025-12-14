package server.game

import proto.dto.Card

class PlayerState(val playerId: Long, val username: String?) {
    var hand: MutableList<Card> = ArrayList()
        private set
    private var hasDeclaredUno = false

    fun hasDeclaredUno(): Boolean {
        return hasDeclaredUno
    }

    fun addCard(card: Card?) {
        hand.add(card!!)
        if (hand.size > 2) {
            hasDeclaredUno = false
        }
    }

    fun removeCard(cardIndex: Int): Card? {
        require(!(cardIndex < 0 || cardIndex >= hand.size)) { "Invalid card index: $cardIndex" }
        val removedCard: Card? = hand.removeAt(cardIndex)
        if (hand.size > 2) {
            hasDeclaredUno = false
        }
        return removedCard
    }

    fun declareUno() {
        if (hand.size == 2) {
            hasDeclaredUno = true
        } else {
            throw IllegalStateException("Can only declare UNO with exactly 2 cards")
        }
    }

    val cardCount: Int
        get() = hand.size
}