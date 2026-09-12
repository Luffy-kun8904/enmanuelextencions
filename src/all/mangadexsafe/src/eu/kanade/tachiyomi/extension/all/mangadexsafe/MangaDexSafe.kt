package eu.kanade.tachiyomi.extension.all.mangadexsafe

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Chapter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MangaDexSafe : HttpSource() {

    override val name = "MangaDex Safe"

    override val lang = "all"

    override val baseUrl = "https://mangadex.org"

    override val supportsLatest = true

    private val apiUrl = "https://api.mangadex.org"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun popularMangaRequest(page: Int): Request {
        return GET(
            "$apiUrl/manga?limit=20&offset=${(page - 1) * 20}" +
                "&contentRating[]=safe" +
                "&availableTranslatedLanguage[]=en&availableTranslatedLanguage[]=es" +
                "&includes[]=cover_art&order[followedCount]=desc",
            headers,
        )
    }

    override fun popularMangaParse(response: Response): MangasPage {
        return parseMangaPage(response)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(
            "$apiUrl/manga?limit=20&offset=${(page - 1) * 20}" +
                "&contentRating[]=safe" +
                "&availableTranslatedLanguage[]=en&availableTranslatedLanguage[]=es" +
                "&includes[]=cover_art&order[latestUploadedChapter]=desc",
            headers,
        )
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        return parseMangaPage(response)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return GET(
            "$apiUrl/manga?title=$encoded&limit=20&offset=${(page - 1) * 20}" +
                "&contentRating[]=safe" +
                "&availableTranslatedLanguage[]=en&availableTranslatedLanguage[]=es" +
                "&includes[]=cover_art",
            headers,
        )
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return parseMangaPage(response)
    }

    private fun parseMangaPage(response: Response): MangasPage {
        val result = json.decodeFromString<MangaListResponse>(response.body.string())
        val mangas = result.data.mapNotNull { item ->
            val title = item.attributes.title["es"]
                ?: item.attributes.title["en"]
                ?: item.attributes.title.values.firstOrNull()
                ?: return@mapNotNull null

            val coverId = item.relationships.firstOrNull { it.type == "cover_art" }?.id
            SManga.create().apply {
                url = "/title/${item.id}"
                this.title = title
                thumbnail_url = coverId?.let {
                    "https://uploads.mangadex.org/covers/${item.id}/$it.jpg"
                }
                description = item.attributes.description["es"]
                    ?: item.attributes.description["en"]
                    ?: item.attributes.description.values.firstOrNull()
                status = when (item.attributes.status) {
                    "ongoing" -> SManga.ONGOING
                    "completed" -> SManga.COMPLETED
                    else -> SManga.UNKNOWN
                }
            }
        }

        return MangasPage(mangas, result.offset + result.limit < result.total)
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(
            "$apiUrl/manga/${manga.url.substringAfterLast('/')}" +
                "?includes[]=cover_art&includes[]=author&includes[]=artist",
            headers,
        )
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val item = json.decodeFromString<MangaResponse>(response.body.string()).data
        val coverId = item.relationships.firstOrNull { it.type == "cover_art" }?.id

        return SManga.create().apply {
            url = "/title/${item.id}"
            title = item.attributes.title["es"]
                ?: item.attributes.title["en"]
                ?: item.attributes.title.values.firstOrNull()
                ?: "Unknown"
            description = item.attributes.description["es"]
                ?: item.attributes.description["en"]
                ?: item.attributes.description.values.firstOrNull()
            author = item.relationships.firstOrNull { it.type == "author" }?.attributes?.name
            artist = item.relationships.firstOrNull { it.type == "artist" }?.attributes?.name
            thumbnail_url = coverId?.let {
                "https://uploads.mangadex.org/covers/${item.id}/$it.jpg"
            }
            status = when (item.attributes.status) {
                "ongoing" -> SManga.ONGOING
                "completed" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
            genre = item.attributes.tags.mapNotNull { tag ->
                tag.attributes.name["en"] ?: tag.attributes.name["es"]
            }.joinToString(", ")
        }
    }

    override fun chapterListRequest(manga: SManga): Request {
        return GET(
            "$apiUrl/manga/${manga.url.substringAfterLast('/')}/feed" +
                "?limit=100&offset=0&translatedLanguage[]=en&translatedLanguage[]=es" +
                "&contentRating[]=safe&order[chapter]=desc&includes[]=scanlation_group",
            headers,
        )
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.decodeFromString<ChapterListResponse>(response.body.string())
        return result.data.map { item ->
            SChapter.create().apply {
                url = "/chapter/${item.id}"
                name = buildString {
                    append("Chapter ")
                    append(item.attributes.chapter ?: "?")
                    item.attributes.title?.takeIf { it.isNotBlank() }?.let {
                        append(" - ")
                        append(it)
                    }
                }
                chapter_number = item.attributes.chapter?.toFloatOrNull() ?: -1f
                date_upload = parseDate(item.attributes.readableAt ?: item.attributes.publishAt)
                scanlator = item.relationships.firstOrNull { it.type == "scanlation_group" }
                    ?.attributes?.name
            }
        }
    }

    override fun pageListRequest(chapter: SChapter): Request {
        return GET(
            "https://api.mangadex.org/at-home/server/${chapter.url.substringAfterLast('/')}",
            headers,
        )
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = json.decodeFromString<AtHomeResponse>(response.body.string())
        val base = result.baseUrl
        val hash = result.chapter.hash

        return result.chapter.dataSaver.mapIndexed { index, filename ->
            Page(
                index,
                imageUrl = "$base/data-saver/$hash/$filename",
            )
        }
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("MangaDex returns direct image URLs in pageListParse")
    }

    private fun parseDate(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)?.time ?: 0L
        }.getOrDefault(0L)
    }

    @Serializable
    data class MangaListResponse(
        val result: String = "",
        val response: String = "",
        val data: List<MangaData> = emptyList(),
        val limit: Int = 20,
        val offset: Int = 0,
        val total: Int = 0,
    )

    @Serializable
    data class MangaResponse(
        val result: String = "",
        val response: String = "",
        val data: MangaData,
    )

    @Serializable
    data class MangaData(
        val id: String,
        val attributes: MangaAttributes,
        val relationships: List<Relationship> = emptyList(),
    )

    @Serializable
    data class MangaAttributes(
        val title: Map<String, String> = emptyMap(),
        val description: Map<String, String> = emptyMap(),
        val status: String = "",
        val tags: List<Tag> = emptyList(),
    )

    @Serializable
    data class Tag(
        val attributes: TagAttributes = TagAttributes(),
    )

    @Serializable
    data class TagAttributes(
        val name: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class Relationship(
        val id: String,
        val type: String,
        val attributes: RelationshipAttributes? = null,
    )

    @Serializable
    data class RelationshipAttributes(
        val name: String? = null,
    )

    @Serializable
    data class ChapterListResponse(
        val data: List<ChapterData> = emptyList(),
    )

    @Serializable
    data class ChapterData(
        val id: String,
        val attributes: ChapterAttributes,
        val relationships: List<Relationship> = emptyList(),
    )

    @Serializable
    data class ChapterAttributes(
        val chapter: String? = null,
        val title: String? = null,
        @SerialName("publishAt") val publishAt: String? = null,
        @SerialName("readableAt") val readableAt: String? = null,
    )

    @Serializable
    data class AtHomeResponse(
        val baseUrl: String,
        val chapter: AtHomeChapter,
    )

    @Serializable
    data class AtHomeChapter(
        val hash: String,
        val dataSaver: List<String> = emptyList(),
    )
}
