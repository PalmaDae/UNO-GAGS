package client.service

import proto.dto.GamePhase

class GamePhaseManager(
    initialPhase: GamePhase = GamePhase.WAITING_TURN
) {
    private var phase: GamePhase = initialPhase

    fun getCurrentPhase(): GamePhase = phase

    fun setPhase(newPhase: GamePhase) {
        val oldPhase = phase
        val valid = isValidTransition(oldPhase, newPhase)

        phase = newPhase

        println("[GamePhaseManager] Phase transition: $oldPhase -> $newPhase")
        if (!valid) {
            println("[GamePhaseManager] Invalid phase transition: $oldPhase -> $newPhase")
        }
    }

    private fun isValidTransition(from: GamePhase, to: GamePhase): Boolean {
        if (from == to) return true

        return when (from) {
            GamePhase.WAITING_TURN -> to == GamePhase.CHOOSING_COLOR || to == GamePhase.DRAWING_CARD || to == GamePhase.FINISHED
            GamePhase.CHOOSING_COLOR -> to == GamePhase.DRAWING_CARD || to == GamePhase.FINISHED
            GamePhase.DRAWING_CARD -> to == GamePhase.WAITING_TURN || to == GamePhase.FINISHED
            GamePhase.FINISHED -> false
        }
    }

    fun isDrawButtonEnabled(isMyTurn: Boolean) =
        isMyTurn && phase == GamePhase.WAITING_TURN


    fun isUnoButtonEnabled(isMyTurn: Boolean) =
        isMyTurn && (phase == GamePhase.WAITING_TURN || phase == GamePhase.DRAWING_CARD)


    fun canInteractWithHand(isMyTurn: Boolean) =
        isMyTurn && (phase == GamePhase.WAITING_TURN || phase == GamePhase.DRAWING_CARD)


    fun shouldShowColorChooser(isMyTurn: Boolean) =
        isMyTurn && phase == GamePhase.CHOOSING_COLOR

    fun isTransitioning() =
        phase == GamePhase.DRAWING_CARD || phase == GamePhase.CHOOSING_COLOR

}