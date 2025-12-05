package view

import enity.Player
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import jdk.internal.vm.ThreadContainers.root

class LobbyView(private val stage: Stage, private val players: List<Player>) {
    lateinit var scene: Scene;

    init {
        val root = VBox(16.0).apply {
            alignment = Pos.CENTER
        }

        players.forEach {root.children.add(Label("${it.name}, ${it.role}"))}

        scene = Scene(root, 400.0, 600.0)
    }
}