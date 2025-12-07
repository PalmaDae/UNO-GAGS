package uno_ui

import uno_proto.dto.Card

/**
 * Tracks player-specific state.
 */
class PlayerModel {
    var playerId: Long? = null
    var username: String = ""
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
        clearHand()
        isReady = false
    }
}
