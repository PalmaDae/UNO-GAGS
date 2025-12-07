package uno_ui

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage

class MainApp : Application() {

    private lateinit var controller: GameController
    private lateinit var scene: Scene
    private var currentScreen: Pane? = null
    private var playerName: String = ""

    private lateinit var loginScreen: LoginScreen
    private lateinit var createRoomScreen: CreateRoomScreen
    private lateinit var joinRoomScreen: JoinRoomScreen
    private lateinit var lobbyScreen: LobbyScreen
    private lateinit var gameScreen: GameScreen

    override fun start(stage: Stage) {
        controller = GameController()
        controller.setOnStateChanged { handleStateChange() }
        controller.setOnChatMessage { handleChatMessage() }

        initScreens()
        scene = Scene(loginScreen, 900.0, 700.0)
        currentScreen = loginScreen

        stage.title = "UNO Game"
        stage.scene = scene
        stage.setOnCloseRequest {
            controller.disconnect()
            Platform.exit()
        }
        stage.show()
    }

    private fun initScreens() {
        loginScreen = LoginScreen(controller) { name -> 
            playerName = name
            showRoomSelect()
        }
        createRoomScreen = CreateRoomScreen(controller) { showRoomSelect() }
        joinRoomScreen = JoinRoomScreen(controller) { showRoomSelect() }
        lobbyScreen = LobbyScreen(controller) { showRoomSelect() }
        gameScreen = GameScreen(controller)
    }

    private fun switchScreen(newScreen: Pane) {
        currentScreen = newScreen
        scene.root = newScreen
    }

    private fun showRoomSelect() {
        val roomSelectScreen = RoomSelectScreen(controller, playerName, 
            { switchScreen(createRoomScreen) }, 
            { switchScreen(joinRoomScreen) }
        )
        switchScreen(roomSelectScreen)
    }

    private fun showLobby() {
        switchScreen(lobbyScreen)
        lobbyScreen.updateLobbyState()
    }

    private fun showGame() {
        switchScreen(gameScreen)
        gameScreen.updateGameState()
    }

    private fun handleStateChange() {
        Platform.runLater {
            val gameState = controller.getCurrentGameState()
            val roomId = controller.getCurrentRoomId()

            when {
                gameState != null -> {
                    if (currentScreen != gameScreen) showGame()
                    else gameScreen.updateGameState()
                }
                roomId != null -> {
                    if (currentScreen != lobbyScreen) showLobby()
                    else lobbyScreen.updateLobbyState()
                }
            }
        }
    }

    private fun handleChatMessage() {
        Platform.runLater {
            if (currentScreen == gameScreen) gameScreen.updateChat()
        }
    }
}

fun main() = Application.launch(MainApp::class.java)
