package client.controller

import javafx.stage.Stage

class PlayerController(
    private val stage: Stage,
    private val gameController: GameController,
    private val isJoin: Boolean,
    private val initialPassword: String? = null
) {
    fun onBackRequested() {
        // Navigation is handled by the view based on isJoin flag
    }

    fun onPlayerDataSubmitted(name: String, avatar: String) {
        if (name.isBlank()) {
            System.err.println("Имя не может быть пустым.")
            return
        }

        if (!gameController.connect()) {
            System.err.println("Не удалось подключиться к серверу!")
            return
        }

        gameController.setUserData(name, avatar)

        if (isJoin) {
            if (initialPassword == null) {
                System.err.println("Невозможно присоединиться: ID комнаты отсутствует.")
                onBackRequested()
                return
            }

            gameController.joinRoom(
                roomId = null,
                username = name,
                avatar = avatar,
                password = initialPassword
            )
        }
    }
}