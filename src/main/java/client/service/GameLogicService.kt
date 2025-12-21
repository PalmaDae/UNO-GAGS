package client.service

import proto.dto.Card
import proto.dto.CardColor
import proto.dto.CardType
import proto.dto.GamePhase
import proto.dto.GameState

class GameLogicService {

    fun canPlayCard(playedCard: Card, gameState: GameState?): Boolean {
        val gs = gameState ?: return true
        val currentTopCard = gs.currentCard ?: return true

        if (playedCard.type == CardType.WILD || playedCard.type == CardType.WILD_DRAW_FOUR) {
            return true
        }

        if (gs.chosenColor != null && gs.chosenColor != CardColor.WILD) {
            return playedCard.color == gs.chosenColor
        }

        val colorMatch = playedCard.color == currentTopCard.color
        val typeMatch = playedCard.type == currentTopCard.type && playedCard.type != CardType.NUMBER
        val numberMatch = (playedCard.type == CardType.NUMBER && currentTopCard.type == CardType.NUMBER) &&
                (playedCard.number == currentTopCard.number)

        return colorMatch || typeMatch || numberMatch
    }

    fun canDrawCard(gameState: GameState?, myPlayerId: Long?): Boolean {
        if (gameState == null || myPlayerId == null) return false
        if (gameState.currentPlayerId != myPlayerId) return false

        return gameState.gamePhase == GamePhase.WAITING_TURN
    }

    fun canSelectColor(gameState: GameState?, myPlayerId: Long?): Boolean {
        if (gameState == null || myPlayerId == null) return false
        if (gameState.currentPlayerId != myPlayerId) return false

        return gameState.gamePhase == GamePhase.CHOOSING_COLOR
    }

    fun requiresColorSelection(card: Card) =
        card.type == CardType.WILD || card.type == CardType.WILD_DRAW_FOUR


    fun getPhaseAfterPlayingCard(card: Card) =
        if (requiresColorSelection(card)) GamePhase.CHOOSING_COLOR else GamePhase.WAITING_TURN

    fun getPhaseAfterDrawing(): GamePhase = GamePhase.DRAWING_CARD
}