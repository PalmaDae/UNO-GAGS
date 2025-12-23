package client.model

import proto.dto.Card

class PlayerModel {

    var playerId: Long? = null
    var username: String = ""
    var avatar: String = ""
    var role: String = "PLAYER"
    var hand: MutableList<Card> = mutableListOf()
    var isReady: Boolean = false
    var selectedCardIndex: Int = -1

    private fun clearHand() {
        hand.clear()
        selectedCardIndex = -1
    }

    fun selectCard(index: Int) {
        selectedCardIndex = if (index in hand.indices) index else -1
    }

    fun updateHand(newHand: List<Card>) {
        hand.clear()
        hand.addAll(newHand)
        selectedCardIndex = -1
    }

    fun removeCardLocally(index: Int) {
        if (index in hand.indices) {
            hand.removeAt(index)
            selectedCardIndex = -1
        }
    }

    fun reset() {
        playerId = null
        username = ""
        avatar = ""
        role = "PLAYER"
        isReady = false
        clearHand()
    }
}