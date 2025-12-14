package client.view

import client.config.StageConfig
import client.controller.GameController
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class GameView(private val stage: Stage) {
    private val controller = GameController(stage)
    lateinit var scene: Scene

    init {
        val centerCard = ImageView(
            Image(javaClass.getResource("/images/cards/red/5.png").toExternalForm())
        ).apply {
            fitWidth = 200.0
            fitHeight = 300.0
        }

        val centerBox = VBox(centerCard).apply {
            alignment = Pos.CENTER
        }


        val hand = HBox(16.0).apply { alignment = Pos.CENTER }

        repeat(6) { index ->
            val cardImg = ImageView(
                Image(javaClass.getResource("/images/cards/blue/$index.png").toExternalForm())
            ).apply {
                fitWidth = 90.0
                fitHeight = 135.0
                setOnMouseClicked { println("Clicked card $index") }
            }
            hand.children.add(cardImg)
        }


        fun closedDeck(count: Int): VBox =
            VBox(8.0).apply {
                alignment = Pos.CENTER
                repeat(count) {
                    children.add(
                        ImageView(
                            Image(javaClass.getResource("/images/cards/back.png").toExternalForm())
                        ).apply {
                            fitWidth = 70.0
                            fitHeight = 105.0
                        }
                    )
                }
            }

        val leftPlayer = closedDeck(4)
        val rightPlayer = closedDeck(4)



        val topPlayer = HBox(8.0).apply {
            alignment = Pos.CENTER
            repeat(4) {
                children.add(
                    ImageView(
                        Image(javaClass.getResource("/images/cards/back.png").toExternalForm())
                    ).apply {
                        fitWidth = 70.0
                        fitHeight = 105.0
                    }
                )
            }
        }


        val unoButton = Button("Say UNO").apply {
            prefWidth = 160.0
            setOnAction { println("UNO pressed") }
        }

        val unoWrapper = VBox(10.0, unoButton).apply {
            alignment = Pos.CENTER
        }


        val exitButton = Button("Exit").apply {
            prefWidth = 120.0
            setOnAction { controller.closedGame() }
        }

        val exitWrapper = HBox(exitButton).apply {
            alignment = Pos.TOP_RIGHT
            style = "-fx-padding: 16;"
        }


        val root = BorderPane()

        root.center = centerBox
        root.bottom = VBox(10.0, unoWrapper, hand).apply {
            alignment = Pos.CENTER
        }

        root.left = leftPlayer
        root.right = rightPlayer
        root.top = VBox(exitWrapper, topPlayer).apply {
            alignment = Pos.CENTER
        }

        root.style = "-fx-padding: 20;"


        scene = Scene(root, StageConfig.getWidth(stage), StageConfig.getHeight(stage))

        scene.stylesheets.add(javaClass.getResource("/css/style.css").toExternalForm())

        scene.setOnKeyPressed { event ->
            when (event.code) {
                KeyCode.SPACE -> unoButton.fire()
                else -> {}
            }
        }
    }
}
