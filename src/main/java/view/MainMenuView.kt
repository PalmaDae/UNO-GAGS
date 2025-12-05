package view

import javafx.scene.Parent
import tornadofx.*;

class MainMenuView : View("UNO") {
    val btnWidth = 200.0

    override val root = vbox {
        spacing = 12.0
        paddingAll = 18.0;

        alignment

        button("Create") {
            prefWidth = btnWidth;
        }
        button("Join") {
            prefWidth = btnWidth;
        }
        button("Exit") {
            prefWidth = btnWidth;
        }
    }
}