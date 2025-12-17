package client.model

import proto.dto.GameState

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