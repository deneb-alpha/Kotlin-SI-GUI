package at.reisisoft.ui

import javafx.application.Platform
import javafx.beans.binding.DoubleExpression
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.Pane
import javafx.scene.text.Font
import javafx.scene.web.WebView
import javafx.stage.*
import java.nio.file.Path
import java.nio.file.Paths

fun runOnUiThread(action: () -> Unit): Unit = Platform.runLater(action)

internal fun Button.closeStageOnClick() {
    onAction = EventHandler<ActionEvent> { closeStageOnClickAction()() }
}

internal fun Button.closeStageOnClickAction(): () -> Unit = {
    this.scene.window.let {
        if (it is Stage)
            it.close()
        else throw IllegalStateException()
    }
}

internal fun Window.showDirectoryChooser(titleString: String, initalPath: Path = Paths.get(".")): Path? =
    DirectoryChooser().apply {
        title = titleString
        initialDirectory = initalPath.toFile()
    }.showDialog(this)?.toPath()

internal fun Window.showFileChooser(
    titleString: String,
    initialPath: Path,
    vararg extensionFilers: FileChooser.ExtensionFilter
): Path? = FileChooser().apply {
    title = titleString
    initialDirectory = initialPath.toFile()
    this.extensionFilters.addAll(*extensionFilers)
}.showOpenDialog(this)?.toPath()

operator fun DoubleExpression.times(double: Double): DoubleExpression = this.multiply(double)
operator fun DoubleExpression.times(int: Int): DoubleExpression = this.multiply(int)

internal fun Pane.preferWindowSize() {
    sceneProperty().addListener { _, _, scene ->
        prefWidthProperty().bind(scene.widthProperty())
        prefHeightProperty().bind(scene.heightProperty())
    }
}

internal fun Label.addDefaultTooltip() {
    tooltip = Tooltip().also {
        it.textProperty().bind(textProperty())
        it.font = Font(it.font.name, 12.0)
        // Cannot change delay in Java 8
    }
}

val UNCAUGHT_EXCEPTION_HANDLER = Thread.UncaughtExceptionHandler { _, t ->
    System.err.println("Unexpected exception!")
    t.printStackTrace()
    JavaFxUtils.showError(t)
}

internal fun Window.showWebView(title: String, html: String) =
    also {
        Stage().apply {
            this.title = title
            initOwner(it)
            initModality(Modality.APPLICATION_MODAL)
            WebView().also { webView ->
                webView.engine.loadContent(html)

                Scene(webView, 500.0, 300.0).also {
                    scene = it
                }
            }
        }.showAndWait()

    }