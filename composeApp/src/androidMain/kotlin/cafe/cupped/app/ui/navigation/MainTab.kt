package cafe.cupped.app.ui.navigation

enum class MainTab(
    val label: String,
    val activeIconName: String,
    val inactiveIconName: String,
) {
    Feed(
        label = "Feed",
        activeIconName = "filled_home",
        inactiveIconName = "outlined_home",
    ),
    Discover(
        label = "Discover",
        activeIconName = "filled_explore",
        inactiveIconName = "outlined_explore",
    ),
    Community(
        label = "Community",
        activeIconName = "filled_people",
        inactiveIconName = "outlined_people",
    ),
    Profile(
        label = "Profile",
        activeIconName = "filled_account_circle",
        inactiveIconName = "outlined_account_circle",
    ),
}
