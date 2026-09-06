package dev.kicker.hometiles.sms

/**
 * a picture message is not one row in the database but several parts.
 *
 * one mms carries its text, its pictures and a layout description as separate parts. what the
 * list and the conversation show has to be put back together from them.
 */
object MmsParts {

    /** one part of a picture message, as the provider hands it over. */
    data class Part(val contentType: String, val text: String? = null, val id: Long = 0)

    private const val TEXT = "text/plain"
    private const val IMAGE = "image/"

    /**
     * the exact type, not a `text/` prefix: an mms also carries `application/smil`, a layout
     * description in markup. by prefix it would stay out, but a wider check would put
     * `<smil><head>` into the list as if somebody had written it.
     */
    fun isText(part: Part): Boolean =
        part.contentType.equals(TEXT, ignoreCase = true)

    fun isImage(part: Part): Boolean =
        part.contentType.startsWith(IMAGE, ignoreCase = true)

    fun hasImage(parts: List<Part>): Boolean = parts.any(::isImage)

    /**
     * the row id of the picture to show.
     *
     * several pictures in one message are possible and rare; the first is shown, and the
     * others are not lost - they lie in the message and open in the phone's own app.
     */
    fun imageId(parts: List<Part>): Long? = parts.firstOrNull(::isImage)?.id

    /** the text of the message, parts in their order, blank ones left out. */
    fun body(parts: List<Part>): String =
        parts.filter(::isText)
            .mapNotNull { it.text?.trim()?.takeIf(String::isNotEmpty) }
            .joinToString(" ")

    /**
     * what stands in the list.
     *
     * a picture without a word is the common case, and an empty row would look like a fault -
     * so the word for a picture stands there instead. with text the text wins: it says more
     * than the word picture, and the picture is seen on opening.
     */
    fun preview(parts: List<Part>, pictureWord: String): String =
        body(parts).ifEmpty { if (hasImage(parts)) pictureWord else "" }
}
