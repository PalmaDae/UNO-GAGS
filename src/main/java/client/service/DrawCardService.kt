package client.service

import proto.dto.GamePhase
import proto.dto.GameState

class DrawCardService(
    private val gameLogicService: GameLogicService,
    private val phaseManager: GamePhaseManager
) {

    sealed class DrawCardAction {
        data object SendDrawRequest : DrawCardAction()
        data class Denied(val reason: String) : DrawCardAction()
    }

    private fun canDrawCard(gameState: GameState?, myPlayerId: Long?): Boolean {
        val isMyTurn = gameState != null && myPlayerId != null && gameState.currentPlayerId == myPlayerId
        return isMyTurn && phaseManager.getCurrentPhase() == GamePhase.WAITING_TURN &&
                gameLogicService.canDrawCard(gameState, myPlayerId)
    }

    fun handleDrawCardAttempt(gameState: GameState?, myPlayerId: Long?): DrawCardAction {
        val canDraw = canDrawCard(gameState, myPlayerId)
        val phase = phaseManager.getCurrentPhase()

        println(
            "[DrawCardService] handleDrawCardAttempt: canDraw=$canDraw, phase=$phase, myPlayerId=$myPlayerId, currentPlayerId=${gameState?.currentPlayerId}"
        )

        return if (canDraw) {
            println("[DrawCardService] SendDrawRequest")
            DrawCardAction.SendDrawRequest
        } else {
            val denied = DrawCardAction.Denied("Cannot draw card in current state")
            println("[DrawCardService] Denied: ${denied.reason}")
            denied
        }
    }

    fun getPhaseAfterDrawing(): GamePhase = gameLogicService.getPhaseAfterDrawing()
}