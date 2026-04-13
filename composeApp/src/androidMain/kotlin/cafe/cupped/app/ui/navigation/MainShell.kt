package cafe.cupped.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cafe.cupped.app.navigation.shell.MainTabId
import cafe.cupped.app.ui.screens.CommunityScreen
import cafe.cupped.app.ui.screens.DiscoverScreen
import cafe.cupped.app.ui.screens.FeedScreen
import cafe.cupped.app.ui.screens.ProfileScreen
import cafe.cupped.app.ui.theme.CuppedColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell() {
    var selectedTab by rememberSaveable { mutableStateOf(MainTabId.Feed) }
    var isLogSheetVisible by rememberSaveable { mutableStateOf(false) }
    var barHeight by remember { mutableStateOf(0.dp) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = isLogSheetVisible) {
        scope.launch {
            sheetState.hide()
            isLogSheetVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CuppedColor.SurfaceApp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = barHeight)
                .testTag("main-shell-content"),
        ) {
            when (selectedTab) {
                MainTabId.Feed -> FeedScreen()
                MainTabId.Discover -> DiscoverScreen()
                MainTabId.Community -> CommunityScreen()
                MainTabId.Profile -> ProfileScreen()
            }
        }

        MainTabBar(
            selectedTab = selectedTab,
            onTabSelected = { tab -> selectedTab = tab },
            onLogClick = { isLogSheetVisible = true },
            onHeightChanged = { barHeight = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (isLogSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isLogSheetVisible = false },
            sheetState = sheetState,
            containerColor = CuppedColor.SurfaceCard,
            contentColor = CuppedColor.TextPrimary,
        ) {
            LogSheetContent(
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        isLogSheetVisible = false
                    }
                },
            )
        }
    }
}
