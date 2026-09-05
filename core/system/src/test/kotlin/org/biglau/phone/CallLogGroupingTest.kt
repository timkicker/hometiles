package org.biglau.phone

import org.biglau.data.CallGrouping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallLogGroupingTest {

    private var nextId = 1L
    private fun call(
        number: String,
        at: Long,
        direction: CallDirection = CallDirection.OUTGOING,
        name: String? = null,
    ) = CallEntry(nextId++, number, name, direction, at, 0)

    @Test
    fun `consecutive calls from the same number are grouped`() {
        // without it a call tried three times stands in the list three times.
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43660", 200), call("+43660", 100)),
        )
        assertEquals(1, groups.size)
        assertEquals(3, groups.first().count)
    }

    @Test
    fun `different numbers stay apart`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300), call("+43512", 200)))
        assertEquals(2, groups.size)
    }

    @Test
    fun `the same number in another spelling counts as the same`() {
        val groups = CallLogGrouping.group(listOf(call("+43 660 123", 300), call("+43660123", 200)))
        assertEquals(1, groups.size)
    }

    @Test
    fun `calls that are not consecutive stay apart`() {
        // otherwise the order in time would shift and "last called" would no longer be a
        // reliable statement.
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43512", 200), call("+43660", 100)),
        )
        assertEquals(3, groups.size)
        assertEquals(listOf("+43660", "+43512", "+43660"), groups.map { it.number })
    }

    @Test
    fun `the newest row stands in front`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 100), call("+43660", 300), call("+43660", 200)))
        assertEquals(300L, groups.first().latest.timestamp)
    }

    @Test
    fun `the name comes from the first row that has one`() {
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300, name = null), call("+43660", 200, name = "Oma")),
        )
        assertEquals("Oma", groups.first().name)
    }

    @Test
    fun `a group with a missed call is recognised as one`() {
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43660", 200, direction = CallDirection.MISSED)),
        )
        assertTrue(groups.first().hasMissed)
        assertEquals(1, CallLogGrouping.onlyMissed(groups).size)
    }

    @Test
    fun `without missed calls the filtered list stays empty`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300), call("+43512", 200)))
        assertTrue(CallLogGrouping.onlyMissed(groups).isEmpty())
    }

    @Test
    fun `withheld numbers are not grouped`() {
        // they arrive without digits. grouping by that would show three different anonymous
        // callers as one person calling three times.
        val groups = CallLogGrouping.group(
            listOf(call("", 300), call("", 200), call("unbekannt", 100)),
        )
        assertEquals(3, groups.size)
    }

    @Test
    fun `deleting a row takes every call bundled in it along`() {
        // otherwise the row reappears after reloading with one entry fewer, and the user
        // takes deleting for broken.
        val groups = CallLogGrouping.group(
            listOf(call("+43660", 300), call("+43660", 200), call("+43660", 100)),
        )
        assertEquals(3, CallLogGrouping.idsOf(groups.first()).size)
    }

    @Test
    fun `the ids of several groups come together`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300), call("+43512", 200)))
        assertEquals(2, CallLogGrouping.idsOf(groups).size)
    }

    @Test
    fun `filtering goes by the allowed call types`() {
        val groups = CallLogGrouping.group(
            listOf(
                call("+43660", 300, direction = CallDirection.MISSED),
                call("+43512", 200, direction = CallDirection.OUTGOING),
            ),
        )
        assertEquals(
            listOf("+43660"),
            CallLogGrouping.visible(groups, setOf(CallDirection.MISSED)).map { it.number },
        )
    }

    @Test
    fun `a group stays visible when one of its calls matches`() {
        val groups = CallLogGrouping.group(
            listOf(
                call("+43660", 300, direction = CallDirection.OUTGOING),
                call("+43660", 200, direction = CallDirection.MISSED),
            ),
        )
        assertEquals(1, CallLogGrouping.visible(groups, setOf(CallDirection.MISSED)).size)
    }

    @Test
    fun `without an allowed type nothing stays visible`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 300)))
        assertTrue(CallLogGrouping.visible(groups, emptySet()).isEmpty())
    }

    @Test
    fun `an empty list gives no groups`() {
        assertTrue(CallLogGrouping.group(emptyList()).isEmpty())
    }

    @Test
    fun `unsorted input is sorted first`() {
        val groups = CallLogGrouping.group(listOf(call("+43660", 100), call("+43512", 300)))
        assertEquals(listOf("+43512", "+43660"), groups.map { it.number })
    }

    @Test
    fun `without grouping every call stands on its own`() {
        // PLAN.md 4.6 names "by nothing" as a view of its own: whoever wants to know when
        // exactly someone called three times needs the three rows singly.
        val groups = CallLogGrouping.group(
            listOf(
                call("+43664111", 300, CallDirection.MISSED),
                call("+43664111", 200, CallDirection.MISSED),
                call("+43664111", 100, CallDirection.MISSED),
            ),
            CallGrouping.NONE,
        )
        assertEquals(3, groups.size)
        assertEquals(listOf(1, 1, 1), groups.map { it.count })
    }

    @Test
    fun `by direction, missed stays apart from answered`() {
        // grouped, the row would carry only the newer call's icon - the other would be gone,
        // and in this list the direction is everything.
        val entries = listOf(
            call("+43664111", 300, CallDirection.MISSED),
            call("+43664111", 200, CallDirection.INCOMING),
            call("+43664111", 100, CallDirection.INCOMING),
        )
        assertEquals(1, CallLogGrouping.group(entries, CallGrouping.NUMBER).size)
        val byDirection = CallLogGrouping.group(entries, CallGrouping.DIRECTION)
        assertEquals(2, byDirection.size)
        assertEquals(CallDirection.MISSED, byDirection[0].latest.direction)
        assertEquals(2, byDirection[1].count)
    }

    @Test
    fun `by number stays the default`() {
        val entries = listOf(
            call("+43664111", 300, CallDirection.MISSED),
            call("+43664111", 200, CallDirection.INCOMING),
        )
        assertEquals(
            CallLogGrouping.group(entries, CallGrouping.NUMBER).size,
            CallLogGrouping.group(entries).size,
        )
    }

    @Test
    fun `without grouping the order stays newest first`() {
        val groups = CallLogGrouping.group(
            listOf(
                call("+43664111", 100, CallDirection.MISSED),
                call("+43664222", 300, CallDirection.MISSED),
            ),
            CallGrouping.NONE,
        )
        assertEquals(listOf(300L, 100L), groups.map { it.latest.timestamp })
    }
}

/**
 * which call types stand in the list at all. PLAN.md 4.6.
 *
 * `visible` stood in the source with tests and was never called by the app - a filter without
 * a switch. converting the stored hide list into the allowed types is where it can go wrong.
 */
class CallTypeFilterTest {

    @Test
    fun `without a hide list everything is visible`() {
        assertEquals(
            CallDirection.entries.toSet(),
            CallLogGrouping.allowedFrom(emptySet()),
        )
    }

    @Test
    fun `a hidden type drops out`() {
        val allowed = CallLogGrouping.allowedFrom(setOf("MISSED"))
        assertTrue(CallDirection.MISSED !in allowed)
        assertTrue(CallDirection.INCOMING in allowed)
    }

    /**
     * unknown names are ignored: a backup from a later version must not empty the call log
     * just because it names a type that does not exist here yet.
     */
    @Test
    fun `an unknown name does not empty the list`() {
        assertEquals(
            CallDirection.entries.toSet(),
            CallLogGrouping.allowedFrom(setOf("VIDEOANRUF_AUS_DER_ZUKUNFT")),
        )
    }

    // a hide list instead of a show list: a type android adds later is visible by itself
    // rather than quietly missing.
    @Test
    fun `hiding everything is possible and gives an empty list`() {
        val all = CallDirection.entries.map { it.name }.toSet()
        assertEquals(emptySet<CallDirection>(), CallLogGrouping.allowedFrom(all))
    }

    @Test
    fun `the default hides nothing`() {
        assertEquals(emptySet<String>(), org.biglau.data.PhoneConfig().hiddenCallTypes)
    }
}
