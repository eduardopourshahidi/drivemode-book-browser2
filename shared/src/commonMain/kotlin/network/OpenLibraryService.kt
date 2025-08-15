package network

import dev.drivemode.techtest.BookModel
import dev.drivemode.techtest.Works
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import database.BookDatabase
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive


@Serializable
data class ApiResponse(
    val works: List<ApiWork>

)
@Serializable
data class ApiWork(
    val title: String,
    val key: String,
    val cover_id: Int? = null,
    val authors: List<ApiAuthor>,
    val first_publish_year: Int? = null
)

@Serializable
data class ApiBookDetail(
    val title: String,
    val description: Description? = null,
)

@Serializable(with = DescriptionSerializer::class)
data class Description(val text: String)

object DescriptionSerializer : KSerializer<Description> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Description", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Description {
        val input = decoder as? JsonDecoder ?: error("Can only deserialize from JSON")
        val element = input.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> Description(element.content) // Handle: "The magic tree house..."
            is JsonObject -> {
                val text = element["value"]?.jsonPrimitive?.content
                    ?: error("Missing 'value' field in object")
                Description(text)
            }
            else -> error("Unexpected JSON format for Description")
        }
    }

    override fun serialize(encoder: Encoder, value: Description) {
        encoder.encodeString(value.text)
    }
}

@Serializable
data class ApiAuthor(
    val name: String
)

class OpenLibraryService (private val httpClient : HttpClient, val bookDatabase: BookDatabase) {

    private val baseUrl = "https://openlibrary.org"

    fun formatTimestampKMP(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "%04d:%02d:%02d %02d:%02d:%02d".format(
            datetime.year,
            datetime.monthNumber,
            datetime.dayOfMonth,
            datetime.hour,
            datetime.minute,
            datetime.second
        )
    }


    suspend fun getBookBySubject(subject: String, page: Int): Works {

        val httpResponse: HttpResponse = httpClient.get("$baseUrl/subjects/$subject.json?limit=10&offset=${page*10}")
        val timestamp = formatTimestampKMP (httpResponse.responseTime.timestamp)

        val response: ApiResponse = httpResponse.body()

        if(page == 0) bookDatabase.clearBooks()
        val books:List<BookModel> = response.works.map { work ->
            var year = work.first_publish_year

            var book = BookModel(
                title = work.title,
                key = work.key.replace("/works/", ""),
                author = work.authors.joinToString(", ") { it.name } ?: "",
                imageUrl = work.cover_id?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
                    ?: ""
            )
            bookDatabase.insertBook( book.key,book.title, book.author, book.imageUrl, year?.toString() ?: "")
            book
        }

        return Works(
            books = books,
            timeStamp = timestamp
        )
    }


    suspend fun getDetailByBook(bookKey: String): String {
        val httpResponse: HttpResponse  = httpClient.get("$baseUrl/works/$bookKey.json")

        val response:ApiBookDetail = httpResponse.body()
        var description = response.description
        var result = description?.text?:"No Description"

        return result
    }
}