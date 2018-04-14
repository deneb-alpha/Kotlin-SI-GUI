package at.reisisoft.sigui.commons.downloads

import at.reisisoft.checkpoint
import at.reisisoft.stream
import at.reisisoft.toSortedSet
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*
import kotlin.collections.HashSet

object PossibleDownloadHelper {

    fun fetchPossibleFor(
        downloadLocation: DownloadLocation,
        vararg downloadTypes: DownloadType
    ): SortedSet<DownloadInformation> =
        if (downloadLocation == DownloadLocation.DAILY)
            possibleDailyDownloads(setOf(*downloadTypes))
        else
            downloadTypes.stream().flatMap { downloadType ->
                when (downloadLocation) {
                    DownloadLocation.ARCHIVE -> possibleArchive(downloadType)
                    DownloadLocation.STABLE -> possibleReleaseVersion(ReleaseType.STABLE)
                    DownloadLocation.FRESH -> possibleReleaseVersion(ReleaseType.FRESH)
                    else -> throw IllegalStateException("Unexpected location $downloadLocation")
                }.stream()
            }.toSortedSet()

    private const val firstDesktopArchiveVersion = "3.3.0.4/"
    private const val lastDesktopArchiveVersion = "latest/"
    private fun possibleArchiveDesktop(baseUrlDocument: Document): SortedSet<DownloadInformation> =
        baseUrlDocument.select("a[href]").let { elements ->
            val downloadVersionInfo = TreeSet<DownloadInformation>()
            var firstVersionSeen = false
            for (e in elements) {
                e.attr("href").let { possibleVersionInformation ->
                    if (possibleVersionInformation == firstDesktopArchiveVersion)
                        firstVersionSeen = true;

                    if (firstVersionSeen) {
                        possibleVersionInformation.substring(0, possibleVersionInformation.length - 1)
                            .let { versionInfo ->
                                downloadVersionInfo.add(
                                    DownloadInformation(
                                        "${baseUrlDocument.location()}$possibleVersionInformation",
                                        versionInfo,
                                        HashSet<DownloadType>().apply {
                                            //Add Support information
                                            if (versionInfo < "3.5")
                                                add(DownloadType.WINDOWSEXE)
                                            else
                                                add(DownloadType.WINDOWS32)

                                            if (versionInfo >= "4.4.3.2")
                                                add(DownloadType.WINDOWS64)
                                        }
                                    )
                                )
                            }
                        if (possibleVersionInformation == lastDesktopArchiveVersion)
                            return downloadVersionInfo
                    }
                }
            }
            return downloadVersionInfo
        }


    private fun possibleArchive(downloadType: DownloadType): SortedSet<DownloadInformation> =
        parseHtmlDocument(DownloadUrls.ARCHIVE).let { rootDocument ->
            return when (downloadType) {
                DownloadType.ANDROID_LIBREOFFICE_ARM, DownloadType.ANDROID_LIBREOFFICE_X86 -> possibleArchiveAndroid(
                    downloadType,
                    "loviewer",
                    rootDocument
                )
                DownloadType.ANDROID_REMOTE -> possibleArchiveAndroid(downloadType, "sdremote", rootDocument)
                else -> possibleArchiveDesktop(rootDocument)
            }
        }

    private fun possibleArchiveAndroid(
        downloadType: DownloadType,
        folderContainsString: String,
        baseUrlDocument: Document
    ): SortedSet<DownloadInformation> = baseUrlDocument.select("a[href~=$folderContainsString]").let {
        it.stream().map {
            it.attr("href").let {
                DownloadInformation(
                    "${baseUrlDocument.location()}$it",
                    it.substring(0, it.length - 1), mutableSetOf(downloadType)
                )
            }
        }.toSortedSet()
    }

    private enum class ReleaseType { STABLE, FRESH }

    private fun possibleReleaseVersion(release: ReleaseType): SortedSet<DownloadInformation> {
        TODO("Not implemented")
    }

    private fun possibleDailyDownloads(wantedDailyBuilds: Set<DownloadType>): SortedSet<DownloadInformation> =
        parseHtmlDocument(DownloadUrls.DAILY).let { rootDocument ->
            rootDocument.select("a[href]").let { aElements ->
                Regex("(master|libreoffice.*?)/").let { firstLevelRegex ->
                    aElements.stream().parallel().map { it.attr("href") }.filter { firstLevelRegex.matches(it) }
                        .map { branchName ->
                            val level2URL = "${rootDocument.location()}$branchName"
                            branchName to parseHtmlDocument(level2URL)
                        }.checkpoint(false)
                        .flatMap { (branchName, level2Document) ->
                            //Build new URL
                            level2Document.select("a[href~=@]").let { thinderboxNames ->
                                thinderboxNames.stream().parallel().map { it.attr("href") }.map { thinderboxName ->
                                    DownloadInformation(
                                        level2Document.location() + thinderboxName + "current/",
                                        "${branchName.substring(
                                            0,
                                            branchName.length - 1
                                        )}-${thinderboxName.substring(
                                            0,
                                            thinderboxName.length - 1
                                        )}", getDownloadTypeFromThinderboxName(thinderboxName)

                                    )
                                }.filter {
                                    it.supportedDownloadTypes.let { supportedTypes ->
                                        !supportedTypes.contains(DownloadType.UNKNOWN) && supportedTypes.any { supportedType ->
                                            wantedDailyBuilds.contains(supportedType)
                                        }
                                    }
                                }
                                    //We now know every possible Thinderbox location. Now check if the Thinderbox is useful.
                                    .filter { dlInfo ->
                                        try {
                                            parseHtmlDocument(dlInfo.baseUrl).select("a[href~=.]")
                                                .let { downloadableElements ->
                                                    return@filter downloadableElements.isNotEmpty()
                                                }


                                        } catch (e: HttpStatusException) {
                                            if (e.statusCode !in 200..299)
                                                return@filter false
                                            throw e
                                        }

                                    }
                            }
                        }.toSortedSet()
                }
            }
        }

    /**
     * Only Windows and Android are supported
     */
    private fun getDownloadTypeFromThinderboxName(thinderboxName: String): Set<DownloadType> =
        when {
            thinderboxName.contains(
                "win",
                true
            ) -> setOf(if (thinderboxName.contains("x86_64")) DownloadType.WINDOWS64 else DownloadType.WINDOWS32)

            thinderboxName.contains("android", true) -> setOf(
                when {
                    thinderboxName.contains("x86") -> DownloadType.ANDROID_LIBREOFFICE_X86
                    else -> DownloadType.ANDROID_LIBREOFFICE_ARM
                //No remote master
                }
            )

            thinderboxName.contains("macos", true) -> setOf(DownloadType.MAC)

            thinderboxName.contains("linux", true) -> {
                val rpm = thinderboxName.contains("rpm", true)
                val deb = thinderboxName.contains("deb", true)
                val x64bit = thinderboxName.contains("x86_64")
                if (x64bit)
                    TreeSet<DownloadType>().apply {
                        if (rpm)
                            add(DownloadType.LINUX_RPM_64)
                        if (deb)
                            add(DownloadType.LINUX_DEB_64)
                    }
                else TreeSet<DownloadType>().apply {
                    if (rpm)
                        add(DownloadType.LINUX_RPM_32)
                    if (deb)
                        add(DownloadType.LINUX_DEB_32)
                }
            }
            else -> {
                println("Cannot infer downloadtype for: $thinderboxName")
                setOf(DownloadType.UNKNOWN)
            }
        }


    private fun parseHtmlDocument(urlAsString: String): Document =
        URL(urlAsString).let { url ->
            getJsoupResponse(urlAsString).parse()
        }


    private fun getJsoupResponse(urlAsString: String): Connection.Response =
        try {
            Jsoup.connect(urlAsString).timeout(CONNECTION_TIMEOUT).execute()
        } catch (ste: SocketTimeoutException) {
            System.err.println("Exception for $urlAsString")
            System.err.println()
            throw ste
        }

    private const val CONNECTION_TIMEOUT = 2000/*2 seconds*/

}