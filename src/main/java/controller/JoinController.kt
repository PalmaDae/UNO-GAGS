package controller

import javafx.scene.Scene
import javafx.scene.input.Clipboard
import javafx.stage.Stage
import view.MainMenuView
import view.PlayerView

class JoinController(private val stage: Stage) {

    fun joinGame(password: String) {
        val playerView = PlayerView(stage)
        stage.scene = playerView.scene;
    }

    fun backTo() {
        val menuView = MainMenuView(stage)
        stage.scene = menuView.scene
    }

    fun pastePassword(): String {
        val clipboard = Clipboard.getSystemClipboard()
        return if (clipboard.hasString()) {
            clipboard.string
        } else {
            ""
        }
    }
}