package view

import javafx.geometry.Pos
import tornadofx.*;

class MainMenuView : View("Main menu") {
    val btnWidth = 200.0

    override val root = vbox() {
        spacing = 12.0
        paddingAll = 18.0;

        alignment = Pos.CENTER

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