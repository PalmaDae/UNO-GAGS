package client.controller

import javafx.stage.Stage
import client.view.CreateView
import client.view.MainMenuView
import client.view.JoinView

class PlayerController(
    private val stage: Stage,
    private val gameController: GameController,
    private val isJoin: Boolean,
    private val initialPassword: String? = null
) {
    fun backButton() {
        if (isJoin) {
            val joinView = JoinView(stage, gameController)
            stage.scene = joinView.scene
        } else {
            val mainView = MainMenuView(stage, gameController)
            stage.scene = mainView.scene
        }
    }

    fun createPlayer(name: String, avatar: String) {
        if (name.isBlank()) {
            System.err.println("Имя не может быть пустым.")
            return
        }

        if (!gameController.connect()) {
            System.err.println("Не удалось подключиться к серверу!")
            return
        }

        if (isJoin) {
            if (initialPassword == null) {
                System.err.println("Невозможно присоединиться: ID комнаты отсутствует.")
                backButton()
                return
            }

            gameController.joinRoom(
                roomId = null,
                username = name,
                avatar = avatar,
                password = initialPassword
            )
        } else {
            val createView = CreateView(stage, gameController)
            stage.scene = createView.scene
        }
    }
}