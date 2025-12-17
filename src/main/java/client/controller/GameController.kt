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
    private val players = mutableListOf<PlayerModel>()
    private var onStateChanged: Runnable? = null
    private var currentRoomId: Long? = null
    var passwordRoom: String? = null
    private var currentUserName: String = "Guest"
    private var currentUserAvatar: String = "default.png"

    fun setUserData(name: String, avatar: String) {
        this.currentUserName = name
        this.currentUserAvatar = avatar
        println("[GameController] User data saved: $name, $avatar")
    }

    init {
        networkClient.setMessageListener(::handleMessage)
    }

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
        networkClient.sendMessage(request, Method.CHOOSE_COLOR)
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
        password: String?,
        maxPlayers: Int,
        allowStuck: Boolean,
        allowStuckCards: Boolean,
        infinityDrawing: Boolean
    ) {

        passwordRoom = password
        val request = CreateRoomRequest(
            avatar = "",
            password = password,
            allowStuck = allowStuck,
            allowStuckCards = allowStuckCards,
            infinityDrawing = infinityDrawing,
            maxPlayers = maxPlayers,
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