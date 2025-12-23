package client.service

import proto.dto.CardColor
import proto.dto.GamePhase
import proto.dto.GameState

class ColorSelectionService(
    private val gameLogicService: GameLogicService,
    private val phaseManager: GamePhaseManager
) {

    sealed class ColorSelectionAction {
        data class SendColor(val color: CardColor) : ColorSelectionAction()
        data class Denied(val reason: String) : ColorSelectionAction()
    }

    fun canSelectColor(gameState: GameState?, myPlayerId: Long?): Boolean {
        val isMyTurn = gameState != null && myPlayerId != null && gameState.currentPlayerId == myPlayerId
        return isMyTurn && phaseManager.getCurrentPhase() == GamePhase.CHOOSING_COLOR &&
                gameLogicService.canSelectColor(gameState, myPlayerId)
    }

    fun handleColorSelection(gameState: GameState?, myPlayerId: Long?, color: CardColor): ColorSelectionAction {
        val canSelect = canSelectColor(gameState, myPlayerId)
        val phase = phaseManager.getCurrentPhase()

        println(
            "[ColorSelectionService] handleColorSelection: canSelect=$canSelect, phase=$phase, myPlayerId=$myPlayerId, currentPlayerId=${gameState?.currentPlayerId}, chosenColor=$color"
        )

        return if (canSelect) {
            println("[ColorSelectionService] SendColor: $color")
            ColorSelectionAction.SendColor(color)
        } else {
            val denied = ColorSelectionAction.Denied("Cannot select color in current state")
            println("[ColorSelectionService] Denied: ${denied.reason}")
            denied
        }
    }

    fun getPhaseAfterColorSelection(): GamePhase = GamePhase.DRAWING_CARD
}