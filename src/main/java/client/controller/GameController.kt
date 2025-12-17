package client.controller

import client.common.NetworkClient
import client.model.*
import client.view.*
import javafx.application.Platform
import javafx.stage.Stage
import proto.common.NetworkMessage
import proto.dto.*
import java.util.logging.Logger

class GameController(private val stage: Stage) {
    private val networkClient = NetworkClient()
    private val playerModel = Player()
    private val roomModel = Room()
    private val gameStateModel = GameStateModel()
    private val players = mutableListOf<Player>()
    private var onStateChanged: Runnable? = null

    init {
        networkClient.setMessageListener(::handleMessage)
    }

    // todo реализовать присоединение к серверу
    fun connect() = networkClient.connect()

    fun disconnect() {
        networkClient.disconnect()
        resetAllModels()
    }

    fun closedGame() {
        val menuView = MainMenuView(stage, gameController = GameController(stage))
        stage.scene = menuView.scene
    }

    fun chooseColor(roomId: Long, color: CardColor) {
        val request = ChooseColorRequest(roomId, color)
        networkClient.sendPayload(request)
    }

    fun setOnStateChanged(callback: Runnable?) {
        onStateChanged = callback
    }

    fun handleCardSelection(cardIndex: Int, card: Card) {
        val isAlreadySelected = playerModel.selectedCardIndex == cardIndex
        val roomId = getCurrentRoomId() ?: return

        if (isAlreadySelected) {
            val isWild = card.type.name.contains("WILD")

            if (isWild) {
            } else {
                playCard(roomId, null)
                playerModel.selectCard(-1)
            }

        } else {
            playerModel.selectCard(cardIndex)
        }

        notifyStateChanged()
    }

    fun getOpponentsInOrder(): List<PlayerDisplayInfo> {
        val gameState = getCurrentGameState() ?: return emptyList()
        val myPlayerId = getMyPlayerId() ?: return emptyList()

        val allPlayersMap = gameState.players

        if (allPlayersMap.size <= 1) {
            return emptyList()
        }

        val playerIdsInOrder = allPlayersMap.keys.toList()

        val myIndex = playerIdsInOrder.indexOf(myPlayerId)

        if (myIndex == -1) {
            return emptyList()
        }

        val numPlayers = playerIdsInOrder.size
        val opponentsInOrder = mutableListOf<PlayerDisplayInfo>()

        for (i in 1 until numPlayers) {
            val opponentIndex = (myIndex + i) % numPlayers
            val opponentId = playerIdsInOrder[opponentIndex]

            val gameInfo = allPlayersMap[opponentId] ?: continue

            val displayInfo = PlayerDisplayInfo(
                userId = opponentId,
                username = gameInfo.username,
                cardCount = gameInfo.cardCount,
                hasUno = gameInfo.hasUno
            )

            opponentsInOrder.add(displayInfo)
        }

        return opponentsInOrder
    }

    private fun resetAllModels() {
        playerModel.reset()
        roomModel.reset()
        gameStateModel.reset()
    }

    fun createRoom(
        maxPlayers: Int,
        allowStuck: Boolean,
        allowStuckCards: Boolean,
        infinityDrawing: Boolean
    ) {
        val request = CreateRoomRequest(
            allowStuck = allowStuck,
            allowStuckCards = allowStuckCards,
            infinityDrawing = infinityDrawing,
            maxPlayers = maxPlayers,
        )
        println("Sending CreateRoomRequest: $request")
        networkClient.sendPayload(request)
    }

    fun joinRoom(roomId: Long, username: String, avatar: String) {
        if (networkClient.isConnected()) {
            val request = JoinRoomRequest(
                roomId = roomId,
                username = username,
                avatar = avatar
            )
            networkClient.sendPayload(request)
        } else {
            System.err.println("Client is not connected. Cannot join room.")
        }
    }

    fun startGame(roomId: Long) {
        val request = StartGameRequest(roomId)
        networkClient.sendPayload(request)
    }

    fun playCard(roomId: Long, chosenColor: CardColor?) {
        if (!playerModel.hasSelectedCard()) {
            System.err.println("[GameController] No card selected or invalid index")
            return
        }

        val request = PlayCardRequest(
            roomId,
            playerModel.selectedCardIndex,
            chosenColor
        )
        networkClient.sendPayload(request)
    }

    fun drawCard(roomId: Long) {
        val request = DrawCardRequest(roomId)
        networkClient.sendPayload(request)
    }

    fun sayUno(roomId: Long) {
        val request = SayUnoRequest(roomId)
        networkClient.sendPayload(request)
    }

    private fun handleMessage(message: NetworkMessage) {
        println("[GameController] Handling payload: ${message::class.simpleName}")

        when (message.payload) {
            is CreateRoomResponse -> handleRoomCreated(message.payload)
            is JoinRoomResponse -> handleJoinRoom(message.payload)
            is LobbyUpdate -> handleLobbyUpdate(message.payload)
            is GameState -> handleGameState(message.payload)
            is PlayerHandUpdate -> handlePlayerHandUpdate(message.payload)
            is ErrorMessage -> handleError(message.payload)
            is OkMessage -> println("[GameController] OK: ${message.payload}")
            is PongMessage -> println("[GameController] Received PONG")
            else -> println("[GameController] Unhandled payload type: ${message.payload::class.simpleName}")
        }
    }

    private fun handleRoomCreated(response: CreateRoomResponse) {
        roomModel.joinRoom(response.roomId)
        println("[GameController] Room created: ${response.roomId}")
        notifyStateChanged()
    }

    private fun handleJoinRoom(response: JoinRoomResponse) {
        if (response.isSuccessful) {
            roomModel.joinRoom(response.roomId)
            logger.info("Joined room: ${response.roomId}")

            Platform.runLater {
                val lobby = LobbyView(stage, gameController = this)
                stage.scene = lobby.scene
            }
        } else
            System.err.println("[GameController] Failed to join room ${response.roomId}")

        notifyStateChanged()
    }

    private fun handleLobbyUpdate(update: LobbyUpdate) {
        roomModel.updateLobby(update)
        println("[GameController] Lobby updated, players: ${update.players.size}")
        notifyStateChanged()
    }

    private fun handleGameState(newState: GameState) {
        gameStateModel.updateState(newState)
        println("[GameController] Game state updated")
        println("  Current player: ${newState.currentPlayerId}")
        println("  Current card: ${newState.currentCard}")
        println("  Players: ${newState.players.size}")
        if (stage.scene != null && stage.scene.root.javaClass.simpleName != "GameViewRoot") {
            Platform.runLater {
                val gameView = GameView(stage, this)
                stage.scene = gameView.scene
            }
        }

        notifyStateChanged()
    }

    private fun handleError(error: ErrorMessage) {
        System.err.println("[GameController] Error from server: ${error.message}")
    }

    private fun notifyStateChanged() {
        onStateChanged?.let { callback ->
            try {
                Platform.runLater(callback)
            } catch (_: IllegalStateException) {
                callback.run()
            }
        }
    }

    private fun handlePlayerHandUpdate(update: PlayerHandUpdate) {
        Platform.runLater {
            playerModel.updateHand(update.hand)
            logger.info("Received and updated player hand: ${update.hand.size} cards.")
        }
    }

    fun getCurrentGameState(): GameState? = gameStateModel.gameState
    fun getCurrentLobbyState(): LobbyUpdate? = roomModel.lobbyState
    fun getCurrentRoomId(): Long? = roomModel.currentRoomId
    fun getMyPlayerId(): Long? = playerModel.playerId
    fun getMyHand(): List<Card> = playerModel.hand.toList()
    fun getSelectedCardIndex(): Int = playerModel.selectedCardIndex
    fun setSelectedCardIndex(index: Int) = playerModel.selectCard(index)

    companion object {
        private val logger = Logger.getLogger(GameController::class.java.name)
    }
}