package app.coreme.messenger.features.chats.domain.usecase

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for message content validation logic (send button enable logic).
 * Mirrors the `value.isNotBlank()` check in MessageInput.
 */
class MessageContentValidationTest {

    private fun canSend(text: String): Boolean = text.isNotBlank()

    @Test
    fun `empty string cannot be sent`() {
        assertFalse(canSend(""))
    }

    @Test
    fun `whitespace-only string cannot be sent`() {
        assertFalse(canSend("   "))
    }

    @Test
    fun `newline-only string cannot be sent`() {
        assertFalse(canSend("\n\n"))
    }

    @Test
    fun `tab-only string cannot be sent`() {
        assertFalse(canSend("\t"))
    }

    @Test
    fun `single character can be sent`() {
        assertTrue(canSend("a"))
    }

    @Test
    fun `normal message can be sent`() {
        assertTrue(canSend("Привіт, як справи?"))
    }

    @Test
    fun `message with leading spaces can be sent`() {
        assertTrue(canSend("  Привіт"))
    }

    @Test
    fun `message with trailing spaces can be sent`() {
        assertTrue(canSend("Привіт  "))
    }

    @Test
    fun `emoji-only message can be sent`() {
        assertTrue(canSend("😊"))
    }

    @Test
    fun `very long message can be sent`() {
        val long = "А".repeat(4000)
        assertTrue(canSend(long))
    }

    @Test
    fun `ukrainian text can be sent`() {
        assertTrue(canSend("Слава Україні"))
    }

    @Test
    fun `mixed content with spaces can be sent`() {
        assertTrue(canSend("   text   "))
    }
}
