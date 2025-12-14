package client

import javafx.application.Application
import client.controller.GameController
import javafx.stage.Stage
import client.view.MainMenuView

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