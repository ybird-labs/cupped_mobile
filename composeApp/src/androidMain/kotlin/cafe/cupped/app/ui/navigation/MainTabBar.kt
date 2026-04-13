package cafe.cupped.app.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.cupped.app.designsystem.icons.AppIcon as AppIconImage
import cafe.cupped.app.designsystem.icons.AppIcon as DesignIcon
import cafe.cupped.app.navigation.shell.MainTabId
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.app.ui.theme.CuppedMotion
import cafe.cupped.app.ui.theme.CuppedSpacing

private val FloatingButtonSize = 56.dp
private val FloatingButtonLift = CuppedSpacing.Lg
private val FloatingButtonRing = 4.dp
private val ActivePillWidth = 24.dp
private val ActivePillHeight = 3.dp
private val ActivePillCorner = 99.dp
private val CenterSlotWidth = 72.dp
private val TabIconSize = 22.dp

@Composable
fun MainTabBar(
    selectedTab: MainTabId,
    onTabSelected: (MainTabId) -> Unit,
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
                    .background(CuppedColor.SurfaceCard.copy(alpha = 0.95f))
                    .background(Color.White.copy(alpha = 0.08f)),
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
                        .padding(top = CuppedSpacing.Sm, bottom = CuppedSpacing.Sm),
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
                        MainTabDisplaySpec.orderedTabs.take(2).forEach { spec ->
                            val tab = spec.id
                            NavItemButton(
                                tab = tab,
                                isActive = selectedTab == tab,
                                onClick = { onTabSelected(tab) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.width(CenterSlotWidth))

                        MainTabDisplaySpec.orderedTabs.drop(2).forEach { spec ->
                            val tab = spec.id
                            NavItemButton(
                                tab = tab,
                                isActive = selectedTab == tab,
                                onClick = { onTabSelected(tab) },
                                modifier = Modifier.weight(1f),
                            )
                        }
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
    tab: MainTabId,
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
    val displaySpec = remember(tab) { tab.displaySpec() }
    val title = stringResource(displaySpec.titleRes)
    val icon = if (isActive) displaySpec.activeIcon else displaySpec.inactiveIcon

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { selected = isActive }
            .testTag("tab-${displaySpec.testTagSuffix}")
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .width(ActivePillWidth)
                .height(ActivePillHeight)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(ActivePillCorner),
                )
                .testTag("tab-indicator-${displaySpec.testTagSuffix}")
                .padding(bottom = CuppedSpacing.Xs),
        )

        AppIconImage(
            icon = icon,
            contentDescription = null,
            tint = if (isActive) CuppedColor.ActionPrimary else CuppedColor.Muted,
            modifier = Modifier
                .size(TabIconSize)
                .scale(iconScale.value)
                .offset(y = iconOffset.value.dp)
                .padding(bottom = 2.dp),
        )

        Text(
            text = title,
            color = if (isActive) CuppedColor.ActionPrimary else CuppedColor.Muted,
            fontSize = CuppedSpacing.CaptionTextSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(if (isActive) 1f else 0.5f),
        )
    }
}

@Composable
private fun LogActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
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
            .testTag("log-action-button")
            .scale(if (isPressed) 0.92f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        AppIconImage(
            icon = DesignIcon.Coffee,
            contentDescription = stringResource(R.string.log_action_button_label),
            tint = CuppedColor.InkInverse,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun MainTabId.indicatorOffset(tabSlotWidth: Dp): Dp {
    val index = MainTabDisplaySpec.orderedTabs.indexOfFirst { it.id == this }
    require(index >= 0) { "Missing display spec for tab $this" }
    val slotStart = (tabSlotWidth * index) + if (index >= 2) CenterSlotWidth else 0.dp
    return slotStart + ((tabSlotWidth - ActivePillWidth) / 2f) - 1.dp
}
