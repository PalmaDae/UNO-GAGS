package client.controller

import client.common.NetworkClient
import client.model.GameStateModel
import client.model.Player
import client.model.Room
import client.view.GameView
import client.view.LobbyView
import client.view.MainMenuView
import javafx.application.Platform
import javafx.stage.Stage
import proto.common.Payload
import proto.dto.*
import java.util.logging.Logger

class GameController(private val stage: Stage) {
    private val networkClient = NetworkClient()
    private val playerModel = Player()
    private val roomModel = Room()
    private val gameStateModel = GameStateModel()
    private var currentRoomId: Long? = null

    companion object {
        private val logger = Logger.getLogger(GameController::class.java.name)
    }

    val players = mutableListOf<Player>()

    fun closedGame() {
        val menuView = MainMenuView(stage, gameController = GameController(stage))
        stage.scene = menuView.scene
    }


    fun chooseColor(roomId: Long, color: CardColor) {
        val request = ChooseColorRequest(roomId, color)
        networkClient.sendPayload(request)
    }

    private var onStateChanged: Runnable? = null

    init {
        networkClient.setMessageListener(::handleMessage)
    }

    fun setOnStateChanged(callback: Runnable?) {
        onStateChanged = callback
    }

    fun createLobby() {
        val lobby = LobbyView(stage, rules = listOf(), gameController = this)
        stage.scene = lobby.scene
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

    fun connect(): Boolean = networkClient.connect()

    fun disconnect() {
        networkClient.disconnect()
        resetAllModels()
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
        if (!networkClient.isConnected()) {
            println("ОШИБКА: Нет подключения к серверу! Пытаюсь подключиться...")
            val connected = connect()
            if (!connected) return
        }
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

    private fun handleMessage(payload: Payload) {
        println("[GameController] Handling payload: ${payload::class.simpleName}")

        when (payload) {
            is CreateRoomResponse -> {
                handleRoomCreated(payload)
                handleCreateRoomResponse(payload)
            }
            is JoinRoomResponse -> handleJoinRoom(payload)
            is LobbyUpdate -> handleLobbyUpdate(payload)
            is GameState -> handleGameState(payload)
            is PlayerHandUpdate -> handlePlayerHandUpdate(payload)
            is RoomsListPayload -> handleRoomsList(payload)
            is JoinRoomResponse -> handleJoinRoom(payload)
            is ErrorMessage -> handleError(payload)
            is OkMessage -> println("[GameController] OK: ${payload.message}")
            is PongMessage -> println("[GameController] Received PONG")
            else -> println("[GameController] Unhandled payload type: ${payload::class.simpleName}")
        }
    }

    fun handleCreateRoomResponse(response: CreateRoomResponse) {
        if (response.isSuccessful) {
            this.currentRoomId = response.roomId
            Platform.runLater {
                val lobbyView = LobbyView(stage, emptyList(), this)
                stage.scene = lobbyView.scene
            }
        } else {
            println("No room")
        }
    }

    fun addPlayer(name: String, avatar: String, isOwner: Boolean = false) {
        playerModel.playerId = System.currentTimeMillis()
        playerModel.username = name
        playerModel.avatar = avatar
        playerModel.role = if (isOwner) "OWNER" else "PLAYER"

        players.add(playerModel)
        createLobby()
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
                val lobby = LobbyView(stage, rules = listOf(), gameController = this)
                stage.scene = lobby.scene
            }
        } else {
            System.err.println("[GameController] Failed to join room ${response.roomId}")
        }

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

    private fun handleRoomsList(roomsList: RoomsListPayload) {
        roomModel.updateAvailableRooms(roomsList.rooms)

        println("[GameController] Received ${roomsList.rooms.size} rooms")

        notifyStateChanged()
    }

    private fun handleError(error: ErrorMessage) {
        System.err.println("[GameController] Error from server: ${error.message}")
    }

    private fun notifyStateChanged() {
        onStateChanged?.let { callback ->
            try {
                Platform.runLater(callback)
            } catch (e: IllegalStateException) {
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
}