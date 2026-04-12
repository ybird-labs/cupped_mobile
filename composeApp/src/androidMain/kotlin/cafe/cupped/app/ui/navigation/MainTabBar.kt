package cafe.cupped.app.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.app.ui.theme.CuppedMotion
import cafe.cupped.app.ui.theme.CuppedSpacing

private val FloatingButtonSize = 56.dp
private val FloatingButtonLift = 18.dp
private val FloatingButtonRing = 4.dp
private val ActivePillWidth = 24.dp
private val ActivePillHeight = 3.dp
private val ActivePillCorner = 99.dp
private val CenterSlotWidth = 72.dp
private val TabIconSize = 21.dp

@Composable
fun MainTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onLogClick: () -> Unit,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                onHeightChanged(with(density) { size.height.toDp() })
            }
            .testTag("main-tab-bar"),
    ) {
        val tabSlotWidth = (maxWidth - CenterSlotWidth - (CuppedSpacing.Base * 2)) / 4f
        val indicatorOffset by animateDpAsState(
            targetValue = selectedTab.indicatorOffset(tabSlotWidth),
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
            ),
            label = "activePillOffset",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FloatingButtonLift),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CuppedColor.SurfaceCard.copy(alpha = 0.95f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CuppedColor.CanvasBorder),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = CuppedSpacing.Base)
                        .padding(top = 6.dp, bottom = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset, y = 0.dp)
                            .width(ActivePillWidth)
                            .height(ActivePillHeight)
                            .background(
                                color = CuppedColor.ActionPrimary,
                                shape = RoundedCornerShape(ActivePillCorner),
                            )
                            .testTag("active-pill"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        NavItemButton(
                            tab = MainTab.Feed,
                            isActive = selectedTab == MainTab.Feed,
                            onClick = { onTabSelected(MainTab.Feed) },
                            modifier = Modifier.weight(1f),
                        )
                        NavItemButton(
                            tab = MainTab.Discover,
                            isActive = selectedTab == MainTab.Discover,
                            onClick = { onTabSelected(MainTab.Discover) },
                            modifier = Modifier.weight(1f),
                        )

                        Spacer(modifier = Modifier.width(CenterSlotWidth))

                        NavItemButton(
                            tab = MainTab.Community,
                            isActive = selectedTab == MainTab.Community,
                            onClick = { onTabSelected(MainTab.Community) },
                            modifier = Modifier.weight(1f),
                        )
                        NavItemButton(
                            tab = MainTab.Profile,
                            isActive = selectedTab == MainTab.Profile,
                            onClick = { onTabSelected(MainTab.Profile) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        LogActionButton(
            onClick = onLogClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = FloatingButtonLift / 2),
        )
    }
}

@Composable
private fun NavItemButton(
    tab: MainTab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconScale = animateFloatAsState(
        targetValue = if (isActive) 1f else 0.85f,
        animationSpec = CuppedMotion.TabSpring,
        label = "tabIconScale",
    )
    val iconOffset = animateFloatAsState(
        targetValue = if (isActive) -2f else 0f,
        animationSpec = CuppedMotion.TabSpring,
        label = "tabIconOffset",
    )
    val icon = remember(isActive, tab) { tab.icon(isActive) }

    Column(
            modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { selected = isActive }
            .testTag("tab-${tab.name.lowercase()}")
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .width(ActivePillWidth)
                .height(ActivePillHeight)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(ActivePillCorner),
                )
                .testTag("tab-indicator-${tab.name.lowercase()}"),
        )

        Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = if (isActive) CuppedColor.ActionPrimary else CuppedColor.Muted,
            modifier = Modifier
                .size(TabIconSize)
                .scale(iconScale.value)
                .offset(y = iconOffset.value.dp),
        )

        Text(
        text = tab.label,
            color = if (isActive) CuppedColor.ActionPrimary else CuppedColor.Muted,
            fontSize = CuppedSpacing.CaptionTextSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(if (isActive) 1f else 0.58f),
        )
    }
}

@Composable
private fun LogActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(FloatingButtonSize)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = CuppedColor.ActionPrimary.copy(alpha = 0.35f),
                spotColor = CuppedColor.ActionPrimary.copy(alpha = 0.45f),
            )
            .background(CuppedColor.ActionPrimary, CircleShape)
            .border(FloatingButtonRing, CuppedColor.SurfaceApp, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag("log-action-button"),
            contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Log a Brew",
            tint = CuppedColor.InkInverse,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun MainTab.icon(isActive: Boolean): ImageVector {
    return when (this) {
        MainTab.Feed -> if (isActive) Icons.Filled.Home else Icons.Outlined.Home
        MainTab.Discover -> if (isActive) Icons.Filled.Search else Icons.Outlined.Search
        MainTab.Community -> if (isActive) Icons.Filled.Person else Icons.Outlined.Person
        MainTab.Profile -> if (isActive) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
    }
}

private fun MainTab.indicatorOffset(tabSlotWidth: Dp): Dp {
    val slotStart = when (this) {
        MainTab.Feed -> 0.dp
        MainTab.Discover -> tabSlotWidth
        MainTab.Community -> (tabSlotWidth * 2) + CenterSlotWidth
        MainTab.Profile -> (tabSlotWidth * 3) + CenterSlotWidth
    }
    return slotStart + ((tabSlotWidth - ActivePillWidth) / 2f) - 1.dp
}
