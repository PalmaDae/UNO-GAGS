package server.game

import proto.dto.Card
import proto.dto.CardColor
import proto.dto.CardType
import java.util.*

object DeckBuilder {
    fun createStandardDeck(): MutableList<Card?> {
        val deck: MutableList<Card?> = ArrayList<Card?>()
        val colors = arrayOf(CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.YELLOW)

        colors.forEach { color ->
            deck.add(
                Card(
                    id = generateCardId(
                        color = color,
                        type = CardType.NUMBER,
                        number = 0
                    ),
                    color = color,
                    type = CardType.NUMBER,
                    number = 0
                )
            )

            repeat(9) { number ->
                deck.add(
                    Card(
                        id = generateCardId(
                            color = color,
                            type = CardType.NUMBER,
                            number = number
                        ),
                        color = color,
                        type = CardType.NUMBER,
                        number = number
                    )
                )

                deck.add(
                    Card(
                        id = generateCardId(
                            color = color,
                            type = CardType.NUMBER,
                            number = number
                        ) + "_2",
                        color = color,
                        type = CardType.NUMBER,
                        number = number
                    )
                )
            }

            deck.add(
                Card(
                    id = generateCardId(
                        color = color,
                        type = CardType.SKIP,
                        number = null
                    ),
                    color = color,
                    type = CardType.SKIP,
                    number = null
                )
            )
            deck.add(
                Card(
                    id = generateCardId(
                        color = color,
                        type = CardType.SKIP,
                        number = null
                    ) + "_2",
                    color = color,
                    type = CardType.SKIP,
                    number = null
                )
            )

            deck.add(
                Card(
                    id = generateCardId(
                        color = color,
                        type = CardType.REVERSE,
                        number = null
                    ),
                    color = color,
                    type = CardType.REVERSE,
                    number = null
                )
            )

            deck.add(
                Card(
                    id = generateCardId(
                        color = color,
                        type = CardType.REVERSE,
                        number = null
                    ) + "_2",
                    color = color,
                    type = CardType.REVERSE,
                    number = null
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
                    id = generateCardId(
                        color = color,
                        type = CardType.DRAW_TWO,
                        number = null
                    ) + "_2",
                    color = color,
                    type = CardType.DRAW_TWO,
                    number = null
                )
            )
        }

        repeat(9) { index ->
            val number = index + 1
            deck.add(
                Card(
                    id = "WILD_$index",
                    color = CardColor.WILD,
                    type = CardType.WILD,
                    number = null
                )
            )

            deck.add(
                Card(
                    id = "WILD_DRAW_FOUR_$index",
                    color = CardColor.WILD,
                    type = CardType.WILD_DRAW_FOUR,
                    number = null
                )
            )
        }

        deck.shuffle()
        return deck
    }

    private fun generateCardId(
        color: CardColor,
        type: CardType,
        number: Int?
    ): String {
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

        val drawPile = ArrayList(fullDeck.subList(1, fullDeck.size))

        val discardPile = ArrayList<Card?>()
        discardPile.add(fullDeck[0])

        return DeckPiles(drawPile, discardPile)
    }

    class DeckPiles(
        private val drawPile: MutableList<Card?>,
        private val discardPile: MutableList<Card?>
    ) {

        val topCard: Card
            get() {
                check(!discardPile.isEmpty()) { "Discard pile is empty" }
                return discardPile[discardPile.size - 1]!!
            }

        fun drawCard(): Card? {
            if (drawPile.isEmpty()) {
                reshuffleDiscardIntoDraw()
            }

            check(!drawPile.isEmpty()) { "No cards available to draw" }

            return drawPile.removeAt(drawPile.size - 1)
        }

        fun setTopCard(card: Card) {
            discardPile[discardPile.size - 1] = card
        }

        fun playCard(card: Card?) {
            discardPile.add(card)
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