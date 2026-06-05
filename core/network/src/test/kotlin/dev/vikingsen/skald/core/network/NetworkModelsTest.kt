package dev.vikingsen.skald.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun testDeserializeLibraryItemMetadata_SeriesAsObject() {
        val jsonString = """
            {
                "title": "Forbidden Mountain",
                "series": {
                    "id": "series-123",
                    "name": "Guardians",
                    "sequence": "1"
                }
            }
        """.trimIndent()

        val metadata = json.decodeFromString<LibraryItemMetadata>(jsonString)
        assertEquals("Forbidden Mountain", metadata.title)
        assertNotNull(metadata.series)
        assertEquals(1, metadata.series?.size)
        assertEquals("series-123", metadata.series?.get(0)?.id)
        assertEquals("Guardians", metadata.series?.get(0)?.name)
        assertEquals("1", metadata.series?.get(0)?.sequence)
    }

    @Test
    fun testDeserializeLibraryItemMetadata_SeriesAsArray() {
        val jsonString = """
            {
                "title": "Forbidden Mountain",
                "series": [
                    {
                        "id": "series-123",
                        "name": "Guardians",
                        "sequence": "1"
                    }
                ]
            }
        """.trimIndent()

        val metadata = json.decodeFromString<LibraryItemMetadata>(jsonString)
        assertEquals("Forbidden Mountain", metadata.title)
        assertNotNull(metadata.series)
        assertEquals(1, metadata.series?.size)
        assertEquals("series-123", metadata.series?.get(0)?.id)
        assertEquals("Guardians", metadata.series?.get(0)?.name)
        assertEquals("1", metadata.series?.get(0)?.sequence)
    }

    @Test
    fun testDeserializeLibraryItemMetadata_SeriesAsNull() {
        val jsonString = """
            {
                "title": "Forbidden Mountain",
                "series": null
            }
        """.trimIndent()

        val metadata = json.decodeFromString<LibraryItemMetadata>(jsonString)
        assertEquals("Forbidden Mountain", metadata.title)
        assertNull(metadata.series)
    }

    @Test
    fun testDeserializeLibraryItemMetadata_SeriesAbsent() {
        val jsonString = """
            {
                "title": "Forbidden Mountain"
            }
        """.trimIndent()

        val metadata = json.decodeFromString<LibraryItemMetadata>(jsonString)
        assertEquals("Forbidden Mountain", metadata.title)
        assertNull(metadata.series)
    }
}
