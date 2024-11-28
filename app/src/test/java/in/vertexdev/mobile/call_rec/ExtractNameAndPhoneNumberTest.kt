package `in`.vertexdev.mobile.call_rec


import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class ExtractNameAndPhoneNumberTest {

    data class ExtractedData(val name: String?, val cleanedPhoneNumber: String?)

    val nameRegex = "^[a-zA-Z\\s@]+(?=[-_\\(\\d])".toRegex() // Matches names up to delimiters or digits.
    val phoneNumberRegex = "\\(?(\\+?\\d{1,4}[-\\s]?\\d{7,15})\\)?(?!\\d{10,12})".toRegex() // Excludes timestamps.


    private fun extractNameAndPhoneNumber(fileName: String): ExtractedData {
        // Remove the file extension
        val baseName = fileName.substringBeforeLast(".mp3")

        // Split by common delimiters to isolate components
        val parts = baseName.split("-", "_", "(", ")")

        var name: String? = null
        var phoneNumber: String? = null

        for (part in parts) {
            val trimmedPart = part.trim()

            // Check if the part is a timestamp (exactly 10 digits in `YYMMDDHHmm` format)
            if (trimmedPart.all { it.isDigit() } && trimmedPart.length == 10) {
                continue // Ignore timestamps
            }
            // Check if the part matches the phone number regex
            else if (phoneNumberRegex.matches(trimmedPart)) {
                phoneNumber = trimmedPart
            }
            // Otherwise, treat it as part of the name
            else if (trimmedPart.isNotEmpty()) {
                name = name?.let { "$it $trimmedPart" } ?: trimmedPart
            }
        }

        return ExtractedData(
            name = name?.takeIf { it.isNotBlank() },
            cleanedPhoneNumber = phoneNumber?.takeIf { it.isNotBlank() }
        )
    }






    @Test
    fun `test extractNameAndPhoneNumber with name and timestamp`() {
        val fileName = "Raghu-2411261142.mp3"
        val result = extractNameAndPhoneNumber(fileName)
        assertEquals("Raghu", result.name)
        assertNull(result.cleanedPhoneNumber)
    }

    @Test
    fun `test extractNameAndPhoneNumber with phone number only`() {
        val fileName = "+918885598006-2411261142.mp3"
        val result = extractNameAndPhoneNumber(fileName)
        assertEquals("+918885598006", result.cleanedPhoneNumber)
        assertNull(result.name)
    }

    @Test
    fun `test extractNameAndPhoneNumber with name and phone number`() {
        val fileName = "Ratna Prasad @usa (00918885598006)_20241116101916.mp3"
        val result = extractNameAndPhoneNumber(fileName)
        assertEquals("Ratna Prasad @usa", result.name)
        assertEquals("00918885598006", result.cleanedPhoneNumber)
    }

    @Test
    fun `test extractNameAndPhoneNumber with no name or phone number`() {
        val fileName = "2411261142.mp3"
        val result = extractNameAndPhoneNumber(fileName)
        assertNull(result.name)
        assertNull(result.cleanedPhoneNumber)
    }

    @Test
    fun `test extractNameAndPhoneNumber with empty filename`() {
        val fileName = ""
        val result = extractNameAndPhoneNumber(fileName)
        assertNull(result.name)
        assertNull(result.cleanedPhoneNumber)
    }

}
