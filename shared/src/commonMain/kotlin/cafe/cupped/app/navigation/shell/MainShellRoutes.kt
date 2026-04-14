package cafe.cupped.app.navigation.shell

import cafe.cupped.app.navigation.Route

fun Route.mainTabOrNull(): MainTabId? = when (this) {
    Route.Feed -> MainTabId.Feed
    Route.Discover -> MainTabId.Discover
    Route.Community -> MainTabId.Community
    Route.Profile -> MainTabId.Profile
    Route.Log,
    is Route.Post,
    is Route.UserProfile,
    is Route.Cafe,
    Route.Login,
    Route.Register,
    is Route.Web,
    -> null
}

fun Route.shellActionOrNull(): MainShellAction? = when (this) {
    Route.Log -> MainShellAction.Log
    Route.Feed,
    Route.Discover,
    Route.Community,
    Route.Profile,
    is Route.Post,
    is Route.UserProfile,
    is Route.Cafe,
    Route.Login,
    Route.Register,
    is Route.Web,
    -> null
}
