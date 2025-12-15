package client

import client.controller.GameController
import client.view.MainMenuView
import javafx.application.Application
import javafx.stage.Stage

fun main() {
    Application.launch(MainMenuApp::class.java)
}

class MainMenuApp : Application() {
    override fun start(primaryStage: Stage) {
        val gameController = GameController(primaryStage)

        val mainMenu = MainMenuView(primaryStage, gameController)
        primaryStage.scene = mainMenu.scene
        primaryStage.title = "UNO Game"
        primaryStage.show()
    }
}