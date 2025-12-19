package client.controller

import client.common.NetworkClient
import client.model.GameStateModel
import client.model.PlayerModel
import client.model.RoomModel
import client.view.GameView
import client.view.LobbyView
import client.view.MainMenuView
import javafx.application.Platform
import javafx.stage.Stage
import proto.common.Method
import proto.common.NetworkMessage
import proto.dto.*
import java.util.logging.Logger

class GameController(private val stage: Stage) {
    private val networkClient = NetworkClient()
    private val playerModel = PlayerModel()
    private val roomModel = RoomModel()
    private val gameStateModel = GameStateModel()
    private var onStateChanged: Runnable? = null
    private var onPlayerHandUpdated: ((List<Card>) -> Unit)? = null
    private var currentRoomId: Long? = null
    var passwordRoom: String? = null
    var currentUserName: String = "Guest"
    var currentUserAvatar: String = "default.png"
    var myPlayerId: Long? = null

    init {
        networkClient.setMessageListener(::handleMessage)
    }

    fun connect() = networkClient.connect()

    fun disconnect() {
        networkClient.disconnect()
        resetAllModels()
    }

    fun setOnPlayerHandUpdated(callback: ((List<Card>) -> Unit)?) {
        onPlayerHandUpdated = callback
    }

    fun setUserData(name: String, avatar: String) {
        this.currentUserName = name
        this.currentUserAvatar = avatar
        println("[GameController] User data saved: $name, $avatar")
    }

    fun closedGame() {
        val menuView = MainMenuView(stage, gameController = GameController(stage))
        stage.scene = menuView.scene
    }

    fun chooseColor(roomId: Long, color: CardColor) {
        updateGamePhase(GamePhase.WAITING_TURN)

        val request = PlayCardRequest(roomId, playerModel.selectedCardIndex, color)
        networkClient.sendMessage(request, Method.PLAY_CARD)

        playerModel.selectCard(-1)
        notifyStateChanged()
    }

    fun setOnStateChanged(callback: Runnable?) {
        onStateChanged = callback
    }



    fun handleCardSelection(cardIndex: Int, card: Card) {
        val isAlreadySelected = playerModel.selectedCardIndex == cardIndex
        val roomId = getCurrentRoomId() ?: return

        if (isAlreadySelected) {
            if (cardIndex !in playerModel.hand.indices) {
                System.err.println("[GameController] Card index out of bounds")
                return
            }

            val currentCard = playerModel.hand[cardIndex]  // ← ПОЛУЧИТЬ ИЗ ТЕКУЩЕЙ РУКИ
            val isWild = currentCard.type.name.contains("WILD")

            if (isWild) {
                // For wild cards, we need to wait for color selection
                // Color selection will be handled separately via chooseColor method
                // This just keeps the card selected so user can choose color
                updateGamePhase(GamePhase.CHOOSING_COLOR)
            } else {
                playCard(roomId, null)
                updateGamePhase(GamePhase.WAITING_TURN)
                playerModel.selectCard(-1)
            }

        } else
            playerModel.selectCard(cardIndex)

        notifyStateChanged()
    }


    private fun updateGamePhase(phase: GamePhase) {
        gameStateModel.gameState?.let {
            val updatedState = it.copy(gamePhase = phase)
            gameStateModel.updateState(updatedState)
            println("[GameController] Phase changed to: $phase")
        }
    }

    fun getOpponentsInOrder(): List<PlayerDisplayInfo> {
        val gameState = getCurrentGameState() ?: return emptyList()
        val myPlayerId = myPlayerId ?: return emptyList()

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
        password: String?,
        maxPlayers: Int,
        allowStuck: Boolean,
        allowStuckCards: Boolean,
        infinityDrawing: Boolean
    ) {

        passwordRoom = password
        val request = CreateRoomRequest(
            avatar = currentUserAvatar,
            password = password,
            allowStuck = allowStuck,
            allowStuckCards = allowStuckCards,
            infinityDrawing = infinityDrawing,
            maxPlayers = maxPlayers,
            username = currentUserName
        )
        println("Sending CreateRoomRequest: $request")
        networkClient.sendMessage(request, Method.CREATE_ROOM)
    }

    fun joinRoom(roomId: Long?, username: String, avatar: String, password: String? = null) {
        if (!networkClient.isConnected()) {
            println("Connecting to server...")
            val success = connect()
            if (!success) {
                println("Failed to connect")
                return
            }
        }

        val request = JoinRoomRequest(
            roomId = roomId,
            password = password,
            username = username,
            avatar = avatar
        )
        networkClient.sendMessage(request, Method.JOIN_ROOM_REQUEST)
    }

    fun startGame(roomId: Long) {
        val request = StartGameRequest(roomId)
        networkClient.sendMessage(request, Method.START_GAME)
    }

    private fun playCard(roomId: Long, chosenColor: CardColor?) {
        val index = playerModel.selectedCardIndex

        val card = playerModel.hand[index]

        if (index !in playerModel.hand.indices) {
            System.err.println("[GameController] CANNOT PLAY: Invalid index $index. Hand size: ${playerModel.hand.size}")
            return
        }

        if (card.type.name.contains("WILD") && chosenColor == null) {
            println("[GameController] Wild card selected, waiting for color choice UI...")
            return
        }

        if (!playerModel.hasSelectedCard()) {
            System.err.println("[GameController] No card selected or invalid index")
            return
        }

        val request = PlayCardRequest(
            roomId,
            playerModel.selectedCardIndex,
            chosenColor
        )
        networkClient.sendMessage(request, Method.PLAY_CARD)
    }

    fun drawCard(roomId: Long) {
        val request = DrawCardRequest(roomId)
        networkClient.sendMessage(request, Method.DRAW_CARD)
    }

    fun sayUno(roomId: Long) {
        val request = SayUnoRequest(roomId)
        networkClient.sendMessage(request, Method.SAY_UNO)
    }

    private fun handleMessage(message: NetworkMessage) {
        println("[GameController] Handling payload: ${message::class.simpleName}")

        when (message.payload) {
            is CreateRoomResponse -> {
                handleRoomCreated(message.payload)
                handleCreateRoomResponse(message.payload)
            }
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

    fun handleCreateRoomResponse(response: CreateRoomResponse) {
        if (response.isSuccessful) {
            this.currentRoomId = response.roomId

            playerModel.username = this.currentUserName
            playerModel.avatar = this.currentUserAvatar

            Platform.runLater {
                val lobbyView = LobbyView(stage, this)
                stage.scene = lobbyView.scene
            }
        } else {
            println("No room")
        }
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
        if (myPlayerId == null) {
            val me = update.players.find { it.username == currentUserName }
            if (me != null) {
                myPlayerId = me.userId
                playerModel.playerId = me.userId
                println("[GameController] ID auto-detected from Lobby: $myPlayerId")
            }
        }

        println("[GameController] Lobby updated, players: ${update.players.size}")
        notifyStateChanged()
    }

    private fun handleGameState(newState: GameState) {
        gameStateModel.updateState(newState)
        if (myPlayerId == null) {
            val me = newState.players.entries.find { it.value.username == currentUserName }
            if (me != null) {
                myPlayerId = me.key
                playerModel.playerId = me.key
                println("[GameController] ID auto-detected from GameState: $myPlayerId")
            }
        }

        Platform.runLater {
            if (stage.scene?.root?.styleClass?.contains("game-screen") != true) {
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
        println("[DEBUG] Server sent cards: ${update.hand.map { it.id }}")
        playerModel.updateHand(update.hand)
            playerModel.selectCard(-1)
            println("[GameController] Hand updated. Notify UI...")
            notifyStateChanged()
    }


    fun getCurrentGameState(): GameState? = gameStateModel.gameState
    fun getCurrentLobbyState(): LobbyUpdate? = roomModel.lobbyState
    fun getCurrentRoomId(): Long? = roomModel.currentRoomId

    fun getMyHand(): List<Card> {
        val handCopy = playerModel.hand.toList()
        println("[GameController] getMyHand(): ${handCopy.size} cards (thread=${Thread.currentThread().name})")
        return handCopy
    }

    fun getSelectedCardIndex(): Int = playerModel.selectedCardIndex

    companion object {
        private val logger = Logger.getLogger(GameController::class.java.name)
    }
}