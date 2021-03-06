package at.reisisoft.sigui.ui

import at.reisisoft.sigui.commons.downloads.LibreOfficeDownloadFileType
import at.reisisoft.ui.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import java.net.URL
import java.nio.file.Path
import java.util.*

class DownloadUiConroller : Initializable {

    protected lateinit var languageSupport: ResourceBundle

    val downloads: EnumMap<LibreOfficeDownloadFileType, String> =
        EnumMap(LibreOfficeDownloadFileType::class.java)

    private lateinit var baseUrl: String
    private lateinit var downloadPath: Path

    private var alreadyInitialized = false

    @FXML
    private lateinit var abort: Button
    @FXML
    private lateinit var startDl: Button
    @FXML
    private lateinit var toFill: VBox
    @FXML
    private lateinit var urlLabel: Label
    @FXML
    private lateinit var pathLabel: Label
    @FXML
    private lateinit var rootPane: Pane

    fun setDownloads(downloads: Map<LibreOfficeDownloadFileType, String>, baseUrl: String, downloadPath: Path) {
        if (alreadyInitialized)
            throw IllegalStateException("Controler has already been initialized")
        alreadyInitialized = true
        this.downloads.putAll(downloads)
        this.baseUrl = baseUrl
        this.downloadPath = downloadPath
        try {
            internalInitialize()
        } catch (t: Throwable) {
            println("Error on initialize!")
            t.printStackTrace()
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        languageSupport = resources!!
    }

    private fun internalInitialize() {
        rootPane.preferWindowSize()
        startDl.closeStageOnClick()
        abort.onAction = EventHandler<ActionEvent> {
            downloads.clear()
            abort.closeStageOnClickAction()()
        }
        //Build main UI
        languageSupport.doLocalizedReplace(ResourceKey.DOWNLAODER_DOWNLOAD_FROM, baseUrl) { finalString ->
            urlLabel.text = finalString
        }
        languageSupport.doLocalizedReplace(ResourceKey.DOWNLAODER_DOWNLOAD_TO, downloadPath) { finalString ->
            pathLabel.text = finalString
        }
        urlLabel.addDefaultTooltip()
        pathLabel.addDefaultTooltip()

        val cancel = languageSupport.getString(ResourceKey.CANCEL)
        downloads.forEach { type, fileName ->
            HBox().apply {
                spacing = 10.0
                alignment = Pos.CENTER_RIGHT
                toFill.children.add(this)
                prefWidthProperty().bind(toFill.widthProperty())
            }.let { container ->
                container.children.let { children ->
                    Label().apply {
                        text = fileName
                        children.add(this)
                        addDefaultTooltip()
                        isWrapText = true
                        textOverrun = OverrunStyle.LEADING_ELLIPSIS
                    }
                    Button().apply {
                        text = cancel
                        children.add(this)
                        minWidth = Region.USE_PREF_SIZE
                        onAction = EventHandler {
                            downloads.remove(type)
                            toFill.children.remove(container)
                        }
                    }
                }
            }
        }
    }
}