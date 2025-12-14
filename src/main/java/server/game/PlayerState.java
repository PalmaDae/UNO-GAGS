package server.game;

import proto.dto.Card;
import proto.dto.CardType;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет состояние игрока в игре UNO.
 * Отслеживает руку игрока, статус объявления UNO и другие специфичные данные игрока.
 */
public class PlayerState {
    private final long playerId;
    private final String username;
    private final List<Card> hand;
    private boolean hasDeclaredUno;
    
    public PlayerState(long playerId, String username) {
        this.playerId = playerId;
        this.username = username;
        this.hand = new ArrayList<>();
        this.hasDeclaredUno = false;
    }
    
    public long getPlayerId() { return playerId; }
    public String getUsername() { return username; }
    public List<Card> getHand() { return new ArrayList<>(hand); }
    public boolean hasDeclaredUno() { return hasDeclaredUno; }
    
    public void addCard(Card card) { 
        hand.add(card);
        // Reset UNO declaration when drawing cards
        if (hand.size() > 2) {
            hasDeclaredUno = false;
        }
    }
    
    public Card removeCard(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= hand.size()) {
            throw new IllegalArgumentException("Invalid card index: " + cardIndex);
        }
        Card removedCard = hand.remove(cardIndex);
        // Reset UNO declaration if player has more than 2 cards after playing
        if (hand.size() > 2) {
            hasDeclaredUno = false;
        }
        return removedCard;
    }
    
    public void declareUno() { 
        if (hand.size() == 2) {
            hasDeclaredUno = true;
        } else {
            throw new IllegalStateException("Can only declare UNO with exactly 2 cards");
        }
    }
    
    public int getCardCount() { return hand.size(); }
    
    /**
     * Рассчитайте значение всех карт в руке этого игрока.
     * Числовые карты: номинал
     * Карты действий: по 20 очков каждая
     * Уайлд-карды: по 50 очков каждому
     */
    public int calculateHandScore() {
        int score = 0;
        for (Card card : hand) {
            if (card.getType() == CardType.NUMBER) {
                score += card.getNumber() != null ? card.getNumber() : 0;
            } else if (card.getType() == CardType.WILD || card.getType() == CardType.WILD_DRAW_FOUR) {
                score += 50;
            } else {
                score += 20;
            }
        }
        return score;
    }
}