import tornadofx.App
import tornadofx.launch
import view.MainMenuView

class MyApp : App(MainMenuView::class)

fun main() {
    launch<MyApp>()
}