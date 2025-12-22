package client.controller

import client.common.NetworkClient
import client.model.GameStateModel
import client.model.PlayerModel
import client.model.RoomModel
import client.service.CardPlayService
import client.service.ColorSelectionService
import client.service.DrawCardService
import client.service.GameLogicService
import client.service.GamePhaseManager
import client.service.GameStateSync
import client.view.CreateView
import client.view.GameView
import client.view.JoinView
import client.view.LobbyView
import client.view.MainMenuView
import client.view.PlayerView
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

    private val phaseManager = GamePhaseManager()
    private val gameLogicService = GameLogicService()
    private val gameStateSync = GameStateSync(gameStateModel, playerModel, phaseManager)
    private val cardPlayService = CardPlayService(gameLogicService, phaseManager)
    private val drawCardService = DrawCardService(gameLogicService, phaseManager)
    private val colorSelectionService = ColorSelectionService(gameLogicService, phaseManager)

    private var onStateChanged: Runnable? = null

    private var pendingJoinPassword: String? = null
    private var pendingCreateFlow: Boolean = false

    private var currentRoomId: Long? = null
    var passwordRoom: String? = null
    var currentUserName: String = "Guest"
    var currentUserAvatar: String = "default.png"
    var myPlayerId: Long? = null

    init {
        networkClient.setMessageListener(::handleMessage)
    }

    fun connect(): Boolean = networkClient.connect()

    fun disconnect() {
        networkClient.disconnect()
        resetAllModels()
    }

    fun setUserData(name: String, avatar: String) {
        currentUserName = name
        currentUserAvatar = avatar
        println("[GameController] User data saved: $name, $avatar")
    }

    fun setOnStateChanged(callback: Runnable?) {
        onStateChanged = callback
    }

    fun closedGame() {
        val menuView = MainMenuView(stage, this)
        stage.scene = menuView.scene
    }

    // =========================
    // Game actions (Views -> Controller)
    // =========================

    fun onCardSelected(cardIndex: Int, card: Card) {
        val roomId = getCurrentRoomId() ?: return

        when (
            val action = cardPlayService.handleCardSelection(
                cardIndex = cardIndex,
                card = card,
                selectedCardIndex = playerModel.selectedCardIndex,
                gameState = gameStateSync.getCurrentGameState(),
                myPlayerId = myPlayerId
            )
        ) {
            is CardPlayService.CardPlayAction.SelectCard -> playerModel.selectCard(action.index)

            is CardPlayService.CardPlayAction.PlayCard -> {
                sendPlayCardRequest(roomId = roomId, cardIndex = action.index, chosenColor = null)

                gameStateSync.syncCardRemoval(action.index)
                gameStateSync.setLocalPhase(gameLogicService.getPhaseAfterPlayingCard(action.card))
            }

            is CardPlayService.CardPlayAction.Denied -> {
                println("[GameController] ${action.reason}")
            }
        }

        notifyStateChanged()
    }

    fun onDrawCardRequested() {
        val roomId = getCurrentRoomId() ?: return

        when (drawCardService.handleDrawCardAttempt(gameStateSync.getCurrentGameState(), myPlayerId)) {
            is DrawCardService.DrawCardAction.SendDrawRequest -> {
                sendDrawCardRequest(roomId)
                gameStateSync.setLocalPhase(drawCardService.getPhaseAfterDrawing())
                notifyStateChanged()
            }

            is DrawCardService.DrawCardAction.Denied -> {
                println("[GameController] Cannot draw card now")
            }
        }
    }

    fun onColorSelected(color: CardColor) {
        val roomId = getCurrentRoomId() ?: return

        when (colorSelectionService.handleColorSelection(gameStateSync.getCurrentGameState(), myPlayerId, color)) {
            is ColorSelectionService.ColorSelectionAction.SendColor -> {
                sendChooseColorRequest(roomId, color)
                gameStateSync.setLocalPhase(colorSelectionService.getPhaseAfterColorSelection())
                notifyStateChanged()
            }

            is ColorSelectionService.ColorSelectionAction.Denied -> {
                println("[GameController] Cannot choose color now")
            }
        }
    }

    fun onSayUnoRequested() {
        val roomId = getCurrentRoomId() ?: return
        val request = SayUnoRequest(roomId)
        networkClient.sendMessage(request, Method.SAY_UNO)
    }

    // =========================
    // Navigation & flow actions (Views -> Controller)
    // =========================

    fun onCreateGameRequested() {
        pendingCreateFlow = true
        pendingJoinPassword = null

        val playerView = PlayerView(stage = stage, gameController = this, isJoin = false)
        stage.scene = playerView.scene
    }

    fun onJoinGameRequested() {
        pendingCreateFlow = false
        pendingJoinPassword = null

        val joinView = JoinView(stage, this)
        stage.scene = joinView.scene
    }

    fun onJoinRequested(roomPassword: String) {
        val key = roomPassword.trim()
        if (key.isBlank()) return

        pendingCreateFlow = false
        pendingJoinPassword = key
        passwordRoom = key

        val playerView = PlayerView(stage = stage, gameController = this, isJoin = false)
        stage.scene = playerView.scene
    }

    fun onPlayerDataSubmitted(name: String, avatar: String) {
        setUserData(name, avatar)

        val joinPassword = pendingJoinPassword
        if (joinPassword != null) {
            pendingJoinPassword = null
            joinRoom(roomId = null, username = name, avatar = avatar, password = joinPassword)
            return
        }

        if (pendingCreateFlow) {
            pendingCreateFlow = false
            val createView = CreateView(stage, this)
            stage.scene = createView.scene
        }
    }

    fun onStartGameRequested() {
        val roomId = getCurrentRoomId() ?: return
        startGame(roomId)
    }

    fun onLeaveRequested() {
        onLeaveGameRequested()
    }

    fun onLeaveGameRequested() {
        disconnect()
        val menuView = MainMenuView(stage, this)
        stage.scene = menuView.scene
    }

    fun onCreateLobbyRequested(maxPlayers: Int, allowStuck: Boolean, allowStuckCards: Boolean, infinityDrawing: Boolean) {
        val password = generatePassword()
        passwordRoom = password

        createRoom(password, maxPlayers, allowStuck, allowStuckCards, infinityDrawing)
    }

    fun onBackRequested() {
        pendingCreateFlow = false
        pendingJoinPassword = null

        val menuView = MainMenuView(stage, this)
        stage.scene = menuView.scene
    }

    fun onExitRequested() {
        disconnect()
        stage.close()
    }

    fun copyPassword(): String = passwordRoom ?: ""

    fun pastePassword(): String = passwordRoom ?: ""

    // =========================
    // Read-only state for Views
    // =========================

    fun getCurrentGameState(): GameState? = gameStateSync.getCurrentGameState()

    fun getCurrentLobbyState(): LobbyUpdate? = roomModel.lobbyState

    fun getCurrentRoomId(): Long? = roomModel.currentRoomId

    fun isMyTurn(): Boolean {
        val gameState = gameStateSync.getCurrentGameState() ?: return false
        val myId = myPlayerId ?: return false
        return gameState.currentPlayerId == myId
    }

    fun getCurrentPhase(): GamePhase = gameStateSync.getCurrentPhase()

    fun isDrawButtonEnabled(): Boolean = phaseManager.isDrawButtonEnabled(isMyTurn())

    fun isUnoButtonEnabled(): Boolean = phaseManager.isUnoButtonEnabled(isMyTurn())

    fun canInteractWithHand(): Boolean = phaseManager.canInteractWithHand(isMyTurn())

    fun shouldShowColorChooser(): Boolean = phaseManager.shouldShowColorChooser(isMyTurn())

    fun getMyHand(): List<Card> {
        val handCopy = playerModel.hand.toList()
        println("[GameController] getMyHand(): ${handCopy.size} cards (thread=${Thread.currentThread().name})")
        return handCopy
    }

    fun getSelectedCardIndex(): Int = playerModel.selectedCardIndex

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

            opponentsInOrder.add(
                PlayerDisplayInfo(
                    userId = opponentId,
                    username = gameInfo.username,
                    cardCount = gameInfo.cardCount,
                    hasUno = gameInfo.hasUno
                )
            )
        }

        return opponentsInOrder
    }

    // =========================
    // Network actions
    // =========================

    private fun ensureConnected(): Boolean {
        if (networkClient.isConnected()) return true

        println("[GameController] Connecting to server...")
        val ok = connect()
        if (!ok) {
            System.err.println("[GameController] Failed to connect")
        }
        return ok
    }

    fun createRoom(
        password: String?,
        maxPlayers: Int,
        allowStuck: Boolean,
        allowStuckCards: Boolean,
        infinityDrawing: Boolean
    ) {
        if (!ensureConnected()) return

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

        println("[GameController] Sending CreateRoomRequest: $request")
        networkClient.sendMessage(request, Method.CREATE_ROOM)
    }

    fun checkIfIAmOwner(): Boolean {
        val lobby = roomModel.lobbyState ?: return false
        val myId = myPlayerId ?: return false
        val me = lobby.players.find { it.userId == myId }
        return me?.isOwner ?: false
    }

    fun joinRoom(roomId: Long?, username: String, avatar: String, password: String? = null) {
        if (!ensureConnected()) return

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

    private fun sendPlayCardRequest(roomId: Long, cardIndex: Int, chosenColor: CardColor?) {
        if (cardIndex == -1) {
            System.err.println("[GameController] CANNOT PLAY: No index selected")
            return
        }

        val request = PlayCardRequest(
            roomId = roomId,
            cardIndex = cardIndex,
            chosenColor = chosenColor
        )

        println("[Sender] Sending PlayCard: index=$cardIndex, color=$chosenColor")
        networkClient.sendMessage(request, Method.PLAY_CARD)

        playerModel.selectCard(-1)
    }

    private fun sendDrawCardRequest(roomId: Long) {
        val request = DrawCardRequest(roomId)
        println("[GameController] Attempting to draw card in room $roomId")
        networkClient.sendMessage(request, Method.DRAW_CARD)
        playerModel.selectCard(-1)
    }

    private fun sendChooseColorRequest(roomId: Long, color: CardColor) {
        val request = ChooseColorRequest(roomId, color)
        networkClient.sendMessage(request, Method.CHOOSE_COLOR)
        playerModel.selectCard(-1)
        println("[GameController] Color $color sent to server for room $roomId")
    }

    private fun handleMessage(message: NetworkMessage) {
        println("[GameController] Handling payload: ${message.payload}")

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
            currentRoomId = response.roomId
            passwordRoom = response.password

            playerModel.username = currentUserName
            playerModel.avatar = currentUserAvatar

            Platform.runLater {
                val lobbyView = LobbyView(stage, this)
                stage.scene = lobbyView.scene
            }
        } else {
            println("[GameController] Room creation failed")
        }
    }

    private fun handleJoinRoom(response: JoinRoomResponse) {
        if (response.isSuccessful) {
            roomModel.joinRoom(response.roomId)
            currentRoomId = response.roomId

            logger.info("Joined room: ${response.roomId}")

            Platform.runLater {
                val lobby = LobbyView(stage, gameController = this)
                stage.scene = lobby.scene
            }
        } else {
            System.err.println("[GameController] Failed to join room ${response.roomId}")
        }

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
        gameStateSync.updateGameState(newState)

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

    private fun handlePlayerHandUpdate(update: PlayerHandUpdate) {
        gameStateSync.updatePlayerHand(update.hand)
        playerModel.selectCard(-1)
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

    private fun resetAllModels() {
        gameStateSync.reset()
        roomModel.reset()
    }

    private fun generatePassword(): String {
        val chars = "0123456789"
        return (1..5).map { chars.random() }.joinToString("")
    }

    companion object {
        private val logger = Logger.getLogger(GameController::class.java.name)
    }
}