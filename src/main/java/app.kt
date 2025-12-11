import controller.GameController
import javafx.application.Application
import javafx.stage.Stage
import view.MainMenuView

fun main() {
    javafx.application.Application.launch(MainMenuApp::class.java)
}

class MainMenuApp : javafx.application.Application() {
    override fun start(primaryStage: Stage) {
        val gameController = GameController(primaryStage)

        val mainMenu = MainMenuView(primaryStage, gameController)
        primaryStage.scene = mainMenu.scene
        primaryStage.title = "UNO Game"
        primaryStage.show()
    }
}