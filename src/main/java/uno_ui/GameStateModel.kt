package uno_ui

import uno_proto.dto.GameState

/**
 * Tracks current game state.
 */
class GameStateModel {
    var gameState: GameState? = null

    fun isInGame(): Boolean = gameState != null

    fun updateState(newState: GameState) {
        gameState = newState
    }

    fun clearState() {
        gameState = null
    }

    fun reset() {
        clearState()
    }
}
