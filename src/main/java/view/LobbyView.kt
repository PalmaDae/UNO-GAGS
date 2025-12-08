package view

import enity.Player
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import jdk.internal.vm.ThreadContainers.root

class LobbyView(private val stage: Stage, private val rules: List<Boolean>) {
    lateinit var scene: Scene;

    init {
        val root = VBox(16.0).apply {
            alignment = Pos.CENTER
        }

        root.children.add(Label("Dae, OWNER"))

        scene = Scene(root, 400.0, 600.0)
    }
}