package dev.kicker.hometiles.apps

/**
 * how an app is named in the config: hidden, recent, allowed, and which notices a tile counts.
 *
 * the main profile keeps the form it always had, `package/activity`, so every config written
 * before work profiles were known reads the same. an app in another profile (a work profile,
 * issue #6) carries that profile's serial number behind an `@`: the same app can sit in both
 * profiles, and hiding one must not hide the other.
 *
 * the serial and not the user id: android keeps the serial for the life of the profile, the id
 * can be handed out again.
 */
object AppKey {

    private const val MARK = '@'

    fun of(packageName: String, activityName: String, profile: Long?): String =
        "$packageName/$activityName" + suffix(profile)

    /** what a tile counts notices for: the package, and the profile when it is not the main one. */
    fun notices(packageName: String, profile: Long?): String = packageName + suffix(profile)

    /** the profile a key names, null for the main one and for keys written before issue #6. */
    fun profileOf(key: String): Long? =
        key.substringAfterLast(MARK, "").takeIf { MARK in key }?.toLongOrNull()

    /** the key without its profile, `package/activity`. */
    fun withoutProfile(key: String): String = key.substringBeforeLast(MARK)

    private fun suffix(profile: Long?): String = if (profile == null) "" else "$MARK$profile"
}
