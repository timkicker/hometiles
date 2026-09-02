package org.biglau.data

object Defaults {

    const val MAIN_ID = "home"

    /** Startbelegung im 2x3-Raster - das Standardraster fuer 349x565 dp. */
    fun mainScreen(): Screen {
        val actions = listOf(
            Builtin.DIALER,
            Builtin.MESSAGES,
            Builtin.CONTACTS,
            Builtin.CAMERA,
            Builtin.APP_LIST,
            Builtin.SETTINGS,
        )
        val cells = actions.mapIndexed { i, builtin ->
            Cell(
                x = i % 2,
                y = i / 2,
                button = Button(action = ButtonAction.Action(builtin)),
            )
        }
        return Screen(id = MAIN_ID, name = "Start", cols = 2, rows = 3, cells = cells)
    }

    /** Rastervorgaben. Die Zellmasse dahinter stehen in PLAN.md 3.2. */
    val layouts: List<Pair<Int, Int>> = listOf(
        1 to 1,
        1 to 2,
        2 to 2,
        2 to 3,
        2 to 4,
        3 to 4,
        3 to 5,
    )
}
