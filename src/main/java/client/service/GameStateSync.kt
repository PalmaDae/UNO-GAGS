package client.service

import client.model.GameStateModel
import client.model.PlayerModel
import proto.dto.Card
import proto.dto.GamePhase
import proto.dto.GameState

class GameStateSync(
    private val gameStateModel: GameStateModel,
    private val playerModel: PlayerModel,
    private val phaseManager: GamePhaseManager
) {
    private var pendingRemovedCardId: String? = null

    fun updateGameState(newState: GameState) {
        val oldPhase = gameStateModel.gameState?.gamePhase
        gameStateModel.updateState(newState)
        phaseManager.setPhase(newState.gamePhase)

        println(
            "[GameStateSync] updateGameState: phase $oldPhase -> ${newState.gamePhase}, currentPlayerId=${newState.currentPlayerId}, chosenColor=${newState.chosenColor}"
        )
    }

    fun updatePlayerHand(hand: List<Card>) {
        val removedId = pendingRemovedCardId
        if (removedId != null && hand.any { it.id == removedId }) {
            println("[GameStateSync] Ignoring stale hand update (removedCardId=$removedId, handSize=${hand.size})")
            return
        }

        playerModel.updateHand(hand)
        pendingRemovedCardId = null

        println("[GameStateSync] updatePlayerHand: handSize=${hand.size}")
    }

    fun syncCardRemoval(cardIndex: Int) {
        val removedId = playerModel.hand.getOrNull(cardIndex)?.id ?: return
        pendingRemovedCardId = removedId
        playerModel.removeCardLocally(cardIndex)

        println("[GameStateSync] syncCardRemoval: index=$cardIndex, removedCardId=$removedId")
    }

    fun reset() {
        println("[GameStateSync] reset")

        pendingRemovedCardId = null
        gameStateModel.reset()
        playerModel.reset()
        phaseManager.setPhase(GamePhase.WAITING_TURN)
    }

    fun getCurrentGameState() = gameStateModel.gameState

    fun getCurrentPhase() =
        gameStateModel.gameState?.gamePhase ?: phaseManager.getCurrentPhase()

    fun setLocalPhase(phase: GamePhase) {
        val oldPhase = getCurrentPhase()

        gameStateModel.gameState?.let { currentState ->
            gameStateModel.updateState(currentState.copy(gamePhase = phase))
        }
        phaseManager.setPhase(phase)

        println("[GameStateSync] setLocalPhase: $oldPhase -> $phase")
    }
}