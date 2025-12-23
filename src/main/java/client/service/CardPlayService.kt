package client.service

import proto.dto.Card
import proto.dto.GameState

class CardPlayService(
    private val gameLogicService: GameLogicService,
    private val phaseManager: GamePhaseManager
) {

    sealed class CardPlayAction {
        data class SelectCard(val index: Int) : CardPlayAction()
        data class PlayCard(val index: Int, val card: Card) : CardPlayAction()
        data class Denied(val reason: String) : CardPlayAction()
    }

    fun handleCardSelection(
        cardIndex: Int,
        card: Card,
        selectedCardIndex: Int,
        gameState: GameState?,
        myPlayerId: Long?
    ): CardPlayAction {
        val isMyTurn = gameState != null && myPlayerId != null && gameState.currentPlayerId == myPlayerId

        println(
            "[CardPlayService] handleCardSelection: index=$cardIndex, selectedIndex=$selectedCardIndex, isMyTurn=$isMyTurn, phase=${phaseManager.getCurrentPhase()}, card=${card.type}/${card.color}/${card.id}"
        )

        if (!phaseManager.canInteractWithHand(isMyTurn)) {
            val denied = CardPlayAction.Denied("Cannot interact with hand in current phase")
            println("[CardPlayService] Denied: ${denied.reason}")
            return denied
        }

        if (selectedCardIndex != cardIndex) {
            println("[CardPlayService] SelectCard: $cardIndex")
            return CardPlayAction.SelectCard(cardIndex)
        }

        if (!gameLogicService.canPlayCard(card, gameState)) {
            val denied = CardPlayAction.Denied("Нельзя положить ${card.id}!")
            println("[CardPlayService] Denied: ${denied.reason}")
            return denied
        }

        println("[CardPlayService] PlayCard: index=$cardIndex, cardId=${card.id}")
        return CardPlayAction.PlayCard(cardIndex, card)
    }
}