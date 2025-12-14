package server.game

import proto.dto.Card
import proto.dto.CardColor
import proto.dto.CardType
import java.util.*

object DeckBuilder {
    fun createStandardDeck(): MutableList<Card?> {
        val deck: MutableList<Card?> = ArrayList<Card?>()
        val colors = arrayOf<CardColor?>(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)

        for (color in colors) {
            deck.add(Card(generateCardId(color!!, CardType.NUMBER, 0), color, CardType.NUMBER, 0))

            for (number in 1..9) {
                deck.add(
                    Card(
                        generateCardId(color, CardType.NUMBER, number),
                        color,
                        CardType.NUMBER,
                        number
                    )
                )
                deck.add(
                    Card(
                        generateCardId(color, CardType.NUMBER, number) + "_2",
                        color,
                        CardType.NUMBER,
                        number
                    )
                )
            }

            deck.add(Card(generateCardId(color, CardType.SKIP, null), color, CardType.SKIP, null))
            deck.add(
                Card(
                    generateCardId(color, CardType.SKIP, null) + "_2",
                    color,
                    CardType.SKIP,
                    null
                )
            )

            deck.add(Card(generateCardId(color, CardType.REVERSE, null), color, CardType.REVERSE, null))
            deck.add(
                Card(
                    generateCardId(color, CardType.REVERSE, null) + "_2",
                    color,
                    CardType.REVERSE,
                    null
                )
            )

            deck.add(
                Card(
                    generateCardId(color, CardType.DRAW_TWO, null),
                    color,
                    CardType.DRAW_TWO,
                    null
                )
            )
            deck.add(
                Card(
                    generateCardId(color, CardType.DRAW_TWO, null) + "_2",
                    color,
                    CardType.DRAW_TWO,
                    null
                )
            )
        }

        for (i in 1..4) {
            deck.add(Card("WILD_$i", CardColor.WILD, CardType.WILD, null))
            deck.add(Card("WILD_DRAW_FOUR_$i", CardColor.WILD, CardType.WILD_DRAW_FOUR, null))
        }

        deck.shuffle()

        return deck
    }

    private fun generateCardId(color: CardColor, type: CardType, number: Int?): String {
        val id = StringBuilder()
        id.append(color.name).append("_")
        id.append(type.name)
        if (number != null) {
            id.append("_").append(number)
        }
        return id.toString()
    }

    fun createDeckPiles(): DeckPiles {
        val fullDeck = createStandardDeck()

        val drawPile: MutableList<Card?> = ArrayList<Card?>(fullDeck.subList(1, fullDeck.size))

        val discardPile: MutableList<Card?> = ArrayList<Card?>()
        discardPile.add(fullDeck[0])

        return DeckPiles(drawPile, discardPile)
    }

    class DeckPiles(drawPile: MutableList<Card?>, discardPile: MutableList<Card?>) {
        private val drawPile: MutableList<Card?> = ArrayList<Card?>(drawPile)
        private val discardPile: MutableList<Card?> = ArrayList<Card?>(discardPile)

        fun drawCard(): Card? {
            if (drawPile.isEmpty()) {
                reshuffleDiscardIntoDraw()
            }

            check(!drawPile.isEmpty()) { "No cards available to draw" }

            return drawPile.removeAt(drawPile.size - 1)
        }

        fun playCard(card: Card?) {
            discardPile.add(card)
        }

        val topCard: Card
            get() {
                check(!discardPile.isEmpty()) { "Discard pile is empty" }
                return discardPile[discardPile.size - 1]!!
            }

        private fun reshuffleDiscardIntoDraw() {
            if (discardPile.size <= 1) {
                return
            }

            val topCard = discardPile.removeAt(discardPile.size - 1)

            drawPile.addAll(discardPile)
            discardPile.clear()
            drawPile.shuffle()

            discardPile.add(topCard)
        }
    }
}