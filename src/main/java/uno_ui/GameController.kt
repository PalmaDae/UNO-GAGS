package uno_ui

import javafx.application.Platform
import uno_proto.common.*
import uno_proto.dto.*
import uno_server.protocol.MessageParser

/**
 * Coordinates between network client and UI models.
 * Acts as the main controller for the client application.
 */
class GameController {
    private val networkClient = NetworkClient()
    private val playerModel = PlayerModel()
    private val roomModel = RoomModel()
    private val gameStateModel = GameStateModel()
    private val chatModel = ChatModel()

    private var onStateChanged: Runnable? = null
    private var onChatMessage: Runnable? = null

    init {
        networkClient.setMessageListener(::handleMessage)
        networkClient.setTextMessageListener(::handleTextMessage)
    }

    fun setOnStateChanged(callback: Runnable?) {
        onStateChanged = callback
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

    // Room operations
    fun createRoom(roomName: String, maxPlayers: Int) {
        val request = CreateRoomRequest(roomName, null, maxPlayers, false)
        networkClient.sendMessage(Method.CREATE_ROOM, request)
    }

    fun joinRoom(roomId: Long) {
        val request = JoinRoomRequest(roomId, null)
        networkClient.sendMessage(Method.JOIN_ROOM, request)
    }

    fun getRooms() {
        networkClient.sendMessage(Method.GET_ROOMS, uno_server.protocol.MessageParser.EmptyPayload)
    }

    // Game operations
    fun startGame() {
        networkClient.sendMessage(Method.START_GAME, uno_server.protocol.MessageParser.EmptyPayload)
    }

    fun playCard(chosenColor: CardColor?) {
        if (!playerModel.hasSelectedCard()) {
            System.err.println("[GameController] No card selected or invalid index")
            return
        }

        val request = PlayCardRequest(playerModel.selectedCardIndex, chosenColor)
        networkClient.sendMessage(Method.PLAY_CARD, request)
    }

    fun drawCard() {
        networkClient.sendMessage(Method.DRAW_CARD, uno_server.protocol.MessageParser.EmptyPayload)
    }

    fun sayUno() {
        networkClient.sendMessage(Method.SAY_UNO, uno_server.protocol.MessageParser.EmptyPayload)
    }

    // Chat operations
    fun sendChat(text: String) {
        // Используем простой текстовый протокол: CHAT_MESSAGE|playerName|text
        val playerName = playerModel.username.ifEmpty { "Player${playerModel.playerId ?: ""}" }
        val message = "CHAT_MESSAGE|$playerName|$text"
        networkClient.sendTextMessage(message)
    }

    // Message handling
    private fun handleMessage(message: NetworkMessage) {
        println("[GameController] Handling message: ${message.method}")

        when (message.method) {
            Method.ROOM_CREATED_SUCCESS -> handleRoomCreated(message.payload as CreateRoomResponse)
            Method.JOIN_ROOM_SUCCESS -> handleJoinRoom(message.payload as JoinRoomResponse)
            Method.LOBBY_UPDATE -> handleLobbyUpdate(message.payload as LobbyUpdate)
            Method.GAME_STATE, Method.GAME_START -> handleGameState(message.payload as GameState)
            Method.ROOMS_LIST -> handleRoomsList(message.payload as uno_server.protocol.MessageParser.RoomsListPayload)
            Method.LOBBY_CHAT, Method.GAME_CHAT -> handleChat(message.payload as ChatMessage)
            Method.ERROR -> handleError(message.payload as uno_server.protocol.MessageParser.ErrorPayload)
            Method.PONG -> println("[GameController] Received PONG")
            else -> println("[GameController] Unhandled message type: ${message.method}")
        }
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

    private fun handleGameState(newState: GameState) {
        gameStateModel.updateState(newState)
        println("[GameController] Game state updated")
        println("  Current player: ${newState.currentPlayerId}")
        println("  Current card: ${newState.currentCard}")
        println("  Players: ${newState.players.size}")
        notifyStateChanged()
    }

    private fun handleRoomsList(roomsList: uno_server.protocol.MessageParser.RoomsListPayload) {
        println("[GameController] Received ${roomsList.rooms?.size ?: 0} rooms")
    }

    private fun handleChat(chat: ChatMessage) {
        chatModel.addMessage(chat)
        notifyChatMessage()
    }

    private fun handleError(error: uno_server.protocol.MessageParser.ErrorPayload) {
        System.err.println("[GameController] Error from server: ${error.message}")
    }
    
    private fun handleTextMessage(text: String) {
        println("[GameController] Handling text message: $text")
        
        // Парсим формат: CHAT_MESSAGE|playerName|text
        val parts = text.split("|", limit = 3)
        if (parts.size >= 3 && parts[0] == "CHAT_MESSAGE") {
            val playerName = parts[1]
            val messageText = parts[2]
            
            // Создаём ChatMessage объект для модели чата
            val chatMessage = ChatMessage(
                0L, 
                playerName, 
                messageText, 
                ChatMessageType.TEXT, 
                null, 
                System.currentTimeMillis()
            )
            
            chatModel.addMessage(chatMessage)
            notifyChatMessage()
        }
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

    // Getters for UI
    fun getCurrentGameState(): GameState? = gameStateModel.gameState
    fun getCurrentLobbyState(): LobbyUpdate? = roomModel.lobbyState
    fun getCurrentRoomId(): Long? = roomModel.currentRoomId
    fun getMyHand(): List<Card> = playerModel.hand.toList()
    fun getSelectedCardIndex(): Int = playerModel.selectedCardIndex
    fun setSelectedCardIndex(index: Int) = playerModel.selectCard(index)
    fun getChatMessages(): List<ChatMessage> = chatModel.getMessages()
    fun isConnected(): Boolean = networkClient.isConnected()
}
