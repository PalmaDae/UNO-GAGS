package client.controller

import client.common.NetworkClient
import client.model.Chat
import client.model.GameState
import client.model.Player
import client.model.Room
import client.view.LobbyView
import client.view.MainMenuView
import javafx.application.Platform
import javafx.stage.Stage
import proto.common.Payload
import proto.dto.Card
import proto.dto.CardColor
import proto.dto.ChatMessage
import proto.dto.CreateRoomRequest
import proto.dto.CreateRoomResponse
import proto.dto.DrawCardRequest
import proto.dto.ErrorMessage
import proto.dto.GetRoomsRequest
import proto.dto.JoinRoomRequest
import proto.dto.JoinRoomResponse
import proto.dto.LobbyUpdate
import proto.dto.OkMessage
import proto.dto.PlayCardRequest
import proto.dto.PongMessage
import proto.dto.RoomsListPayload
import proto.dto.SayUnoRequest
import proto.dto.StartGameRequest

class GameController(private val stage: Stage) {
    private val networkClient = NetworkClient()
    private val playerModel = Player()
    private val roomModel = Room()
    private val gameStateModel = GameState()
    private val chatModel = Chat()
    val players = mutableListOf<Player>();

    fun closedGame() {
        val menuView = MainMenuView(stage, gameController = GameController(stage))
        stage.scene = menuView.scene
    }


    private var onStateChanged: Runnable? = null
    private var onChatMessage: Runnable? = null

    init {
        networkClient.setMessageListener(::handleMessage)
    }

    fun setOnStateChanged(callback: Runnable?) {
        onStateChanged = callback
    }

    fun createLobby() {
        val lobby = LobbyView(stage, rules = listOf())
        stage.scene = lobby.scene;
    }

    fun setOnChatMessage(callback: Runnable?) {
        onChatMessage = callback
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
        chatModel.clear()
    }

    fun createRoom(roomName: String, maxPlayers: Int) {
        val request = CreateRoomRequest(roomName, null, maxPlayers, false)
        networkClient.sendMessage(request)
    }

    fun joinRoom(roomId: Long) {
        val request = JoinRoomRequest(roomId, null)
        networkClient.sendMessage(request)
    }

    fun getRooms() {
        networkClient.sendMessage(GetRoomsRequest())
    }

    fun startGame(roomId: Long) {
        val request = StartGameRequest(roomId)
        networkClient.sendMessage(request)
    }

    fun playCard(roomId: Long, chosenColor: CardColor?) {
        if (!playerModel.hasSelectedCard()) {
            System.err.println("[GameController] No card selected or invalid index")
            return
        }

        val request = PlayCardRequest(playerModel.selectedCardIndex, chosenColor)
        networkClient.sendMessage(request)
    }

    fun drawCard(roomId: Long) {
        val request = DrawCardRequest(roomId)
        networkClient.sendMessage(request)
    }

    fun sayUno(roomId: Long) {
        val request = SayUnoRequest(roomId)
        networkClient.sendMessage(request)
    }

    fun sendChat(roomId: Long, text: String) {
        val playerName = playerModel.username.ifEmpty { "Player${playerModel.playerId ?: ""}" }
        val message = ChatMessage(
            senderId = playerModel.playerId ?: 0L,
            senderName = playerName,
            content = text
        )
        networkClient.sendMessage(message)
    }

    private fun handleMessage(payload: Payload) {
        println("[GameController] Handling payload: ${payload::class.simpleName}")

        when (payload) {
            is CreateRoomResponse -> handleRoomCreated(payload)
            is JoinRoomResponse -> handleJoinRoom(payload)
            is LobbyUpdate -> handleLobbyUpdate(payload)
//            is GameState -> handleGameState(payload)
            is RoomsListPayload -> handleRoomsList(payload)
            is ChatMessage -> handleChat(payload)
            is ErrorMessage -> handleError(payload)
            is OkMessage -> println("[GameController] OK: ${payload.message}")
            is PongMessage -> println("[GameController] Received PONG")
            else -> println("[GameController] Unhandled payload type: ${payload::class.simpleName}")
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
        roomModel.joinRoom(response.roomId)
        println("[GameController] Joined room: ${response.roomId}")
        notifyStateChanged()
    }

    private fun handleLobbyUpdate(update: LobbyUpdate) {
        roomModel.updateLobby(update)
        println("[GameController] Lobby updated, players: ${update.players.size}")
        notifyStateChanged()
    }

//    private fun handleGameState(newState: GameState) {
//        gameStateModel.updateState(newState)
//        println("[GameController] Game state updated")
//        println("  Current player: ${newState.currentPlayerId}")
//        println("  Current card: ${newState.currentCard}")
//        println("  Players: ${newState.players.size}")
//        notifyStateChanged()
//    }

    private fun handleRoomsList(roomsList: RoomsListPayload) {
        println("[GameController] Received ${roomsList.rooms.size} rooms")
    }

    private fun handleChat(chat: ChatMessage) {
        chatModel.addMessage(chat)
        notifyChatMessage()
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

    private fun notifyChatMessage() {
        onChatMessage?.let { callback ->
            try {
                Platform.runLater(callback)
            } catch (e: IllegalStateException) {
                callback.run()
            }
        }
    }

//    fun getCurrentGameState(): GameState? = gameStateModel.gameState
    fun getCurrentLobbyState(): LobbyUpdate? = roomModel.lobbyState
    fun getCurrentRoomId(): Long? = roomModel.currentRoomId
    fun getMyPlayerId(): Long? = playerModel.playerId
    fun getMyHand(): List<Card> = playerModel.hand.toList()
    fun getSelectedCardIndex(): Int = playerModel.selectedCardIndex
    fun setSelectedCardIndex(index: Int) = playerModel.selectCard(index)
    fun getChatMessages(): List<ChatMessage> = chatModel.getMessages()
    fun isConnected(): Boolean = networkClient.isConnected()
}