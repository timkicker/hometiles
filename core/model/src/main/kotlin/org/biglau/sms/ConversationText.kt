package org.biglau.sms

/**
 * text size inside a conversation, separate from the global one on purpose: a message is
 * read up close, a tile in passing.
 */
object ConversationText {

    val CHOICES = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    /** a backup from a later release may carry a scale that is not on offer here. */
    fun scale(value: Float): Float = value.coerceIn(CHOICES.first(), CHOICES.last())
}
