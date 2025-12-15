package client.model

import proto.dto.Card

class Player {

    var playerId: Long? = null
    var username: String = ""
    var avatar: String = ""
    var role: String = "PLAYER"

    var hand: MutableList<Card> = mutableListOf()
    var isReady: Boolean = false
    var selectedCardIndex: Int = -1

    fun clearHand() {
        hand.clear()
        selectedCardIndex = -1
    }

    fun selectCard(index: Int) {
        selectedCardIndex = if (index in hand.indices) index else -1
    }

    fun hasSelectedCard(): Boolean = selectedCardIndex in hand.indices

    fun getSelectedCard(): Card? = hand.getOrNull(selectedCardIndex)

    fun reset() {
        playerId = null
        username = ""
        avatar = ""
        role = "PLAYER"
        clearHand()
        isReady = false
    }
}

