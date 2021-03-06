package at.reisisoft.sigui.settings

import at.reisisoft.sigui.OSUtils
import at.reisisoft.sigui.commons.downloads.DownloadInformation
import at.reisisoft.sigui.commons.downloads.DownloadLocation
import at.reisisoft.sigui.commons.downloads.DownloadType
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.collections.HashMap

internal data class SiGuiSetting(
    val downloadSelection: Pair<DownloadLocation, DownloadInformation?>? = null,
    @SerializedName("rootInstallationFolder")
    private var intRootInstallationFolder: Path? = null,
    @SerializedName("downloadFolder")
    private var intDownloadFolder: Path? = null,
    @SerializedName("shortcutFolder")
    private var intShortcutDir: Path? = null,
    val installName: String = "",
    val createDesktopShortCut: Boolean = true,
    val installFileMain: Path? = null,
    val installFileHelp: Path? = null,
    val installFileSdk: Path? = null,
    val hpLanguage: Locale? = null,
    val uiLanguage: Locale = Locale.getDefault(),
    val downloadTypes: List<DownloadType> = OSUtils.CURRENT_OS.downloadTypesForOS(),
    val downloadedVersions: Map<DownloadLocation, Set<DownloadInformation>> = emptyMap(),
    val availableHpLanguages: Collection<Locale> = emptyList(),
    val managedInstalledVersions: Map<String/*Displayname*/, Array<Path>/*List of files / folders, which should be deleted*/> = emptyMap()
) {
    internal fun persist() = storeSettings(this)

    val downloadFolder: Path
        get() {
            if (intDownloadFolder == null)
                intDownloadFolder = Files.createTempDirectory("si-gui-download")

            return intDownloadFolder!!
        }
    val rootInstallationFolder: Path
        get() {
            if (intRootInstallationFolder == null)
                intRootInstallationFolder = Files.createTempDirectory("si-gui-installs")
            return intRootInstallationFolder!!
        }

    val shortcutDir: Path
        get() {
            if (intShortcutDir == null)
                intShortcutDir = Paths.get(System.getProperty("user.home"), "Desktop")
            return intShortcutDir!!
        }
}

internal fun <K, V> Map<K, V>.asMutableMap(): MutableMap<K, V> = if (this is MutableMap<K, V>) this else HashMap(this)

internal fun storeSettings(settings: SiGuiSetting): Unit = Files.newBufferedWriter(
    SETTINGS_PATH, DEFAULT_CHARSET, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
).use {
    println("Storing settings")
    JSON.toJson(settings, it)
}

internal fun loadSettings(): SiGuiSetting {
    println("Loading setings")
    if (!Files.exists(SETTINGS_PATH))
        return SiGuiSetting()
    else
        return Files.newBufferedReader(SETTINGS_PATH, DEFAULT_CHARSET).use {
            JSON.fromJson(it, SiGuiSetting::class.java)
        }
}

internal val SETTINGS_PATH by lazy {
    Paths.get(".", "si-gui.settings.json").also {
        println("Settings path: ${it.toAbsolutePath().normalize()}")
    }
}

private val JSON by lazy(GsonBuilder().registerTypeHierarchyAdapter(Path::class.java,
    object : JsonDeserializer<Path>, JsonSerializer<Path> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Path =
            Paths.get(json.asString)


        override fun serialize(src: Path, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            JsonPrimitive(src.toString())
    }
)::create)

private val DEFAULT_CHARSET = StandardCharsets.UTF_8!!