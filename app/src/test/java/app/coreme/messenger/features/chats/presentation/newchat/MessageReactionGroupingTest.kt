package app.coreme.messenger.features.chats.presentation.newchat

import app.coreme.messenger.features.chats.domain.model.Reaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for reaction grouping logic in MessageBubble (groupedReactions function).
 */
class MessageReactionGroupingTest {

    private fun groupedReactions(reactions: List<Reaction>): Map<String, List<Reaction>> =
        reactions.groupBy { it.emoji }

    private fun makeReaction(userId: String, emoji: String) = Reaction(
        emoji = emoji,
        userId = userId,
    )

    @Test
    fun `empty list produces empty map`() {
        val result = groupedReactions(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single reaction produces single-entry map`() {
        val reaction = makeReaction("user-1", "👍")
        val result = groupedReactions(listOf(reaction))
        assertEquals(1, result.size)
        assertEquals(1, result["👍"]?.size)
    }

    @Test
    fun `two same emoji reactions are grouped together`() {
        val r1 = makeReaction("user-1", "❤️")
        val r2 = makeReaction("user-2", "❤️")
        val result = groupedReactions(listOf(r1, r2))
        assertEquals(1, result.size)
        assertEquals(2, result["❤️"]?.size)
    }

    @Test
    fun `different emoji reactions create separate groups`() {
        val r1 = makeReaction("user-1", "👍")
        val r2 = makeReaction("user-2", "❤️")
        val result = groupedReactions(listOf(r1, r2))
        assertEquals(2, result.size)
        assertEquals(1, result["👍"]?.size)
        assertEquals(1, result["❤️"]?.size)
    }

    @Test
    fun `mixed reactions group correctly`() {
        val reactions = listOf(
            makeReaction("u1", "😂"),
            makeReaction("u2", "😂"),
            makeReaction("u3", "😂"),
            makeReaction("u4", "👍"),
            makeReaction("u5", "❤️"),
            makeReaction("u6", "❤️"),
        )
        val result = groupedReactions(reactions)
        assertEquals(3, result.size)
        assertEquals(3, result["😂"]?.size)
        assertEquals(1, result["👍"]?.size)
        assertEquals(2, result["❤️"]?.size)
    }

    @Test
    fun `grouping preserves individual reaction data`() {
        val reaction = makeReaction("user-42", "🔥")
        val result = groupedReactions(listOf(reaction))
        val group = result["🔥"]!!
        assertEquals("user-42", group.first().userId)
        assertEquals("🔥", group.first().emoji)
    }

    @Test
    fun `user can check if they reacted with specific emoji`() {
        val myUserId = "me-123"
        val reactions = listOf(
            makeReaction(myUserId, "👍"),
            makeReaction("other", "👍"),
            makeReaction("other2", "❤️"),
        )
        val result = groupedReactions(reactions)
        val myReaction = result["👍"]?.any { it.userId == myUserId } ?: false
        assertTrue(myReaction)
    }
}
