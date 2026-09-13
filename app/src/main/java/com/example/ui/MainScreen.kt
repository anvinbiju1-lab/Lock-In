package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserProfileRepository
import com.example.ui.blocker.AppBlockerScreen
import com.example.ui.blocker.AppBlockerViewModel
import com.example.ui.habits.HabitViewModel
import com.example.ui.habits.HabitsScreen
import com.example.ui.onboarding.FirstTimePermissionDialog
import com.example.ui.profile.UserProfileDialog
import com.example.ui.sleep.SleepScreen
import com.example.ui.sleep.SleepViewModel
import com.example.ui.stats.FocusStatsScreen
import com.example.ui.stats.FocusStatsViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.UsageStatsHelper

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HABITS("Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "nav_tab_habits"),
    BLOCKER("Blocker", Icons.Filled.Shield, Icons.Outlined.Shield, "nav_tab_blocker"),
    SLEEP("Sleep", Icons.Filled.Bedtime, Icons.Outlined.Bedtime, "nav_tab_sleep"),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_tab_stats")
}

@Composable
fun MainScreen(
    initialTab: AppTab = AppTab.HABITS,
    habitViewModel: HabitViewModel = viewModel(),
    blockerViewModel: AppBlockerViewModel = viewModel(),
    sleepViewModel: SleepViewModel = viewModel(),
    statsViewModel: FocusStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val profileRepo = remember { UserProfileRepository(context) }
    val userProfile by profileRepo.profileState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(initialTab) }
    var isProfileDialogOpen by remember { mutableStateOf(false) }

    // First time launch & permissions checking state
    var hasUsageStats by remember { mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(context)) }
    var hasOverlay by remember { mutableStateOf(UsageStatsHelper.hasOverlayPermission(context)) }
    val essentialGranted = hasUsageStats && hasOverlay

    // Trigger onboarding dialog automatically on first launch or if permissions not completed yet
    var isPermissionDialogOpen by remember {
        mutableStateOf(
            !profileRepo.hasCompletedPermissionOnboarding() || !essentialGranted
        )
    }

    fun refreshAllPermissions() {
        hasUsageStats = UsageStatsHelper.hasUsageStatsPermission(context)
        hasOverlay = UsageStatsHelper.hasOverlayPermission(context)
        blockerViewModel.checkPermissions(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        refreshAllPermissions()
    }

    val avatarColor = remember(userProfile.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(userProfile.avatarColorHex))
        } catch (e: Exception) {
            PurplePrimary
        }
    }

    val avatarInteractionSource = remember { MutableInteractionSource() }
    val isAvatarPressed by avatarInteractionSource.collectIsPressedAsState()
    val avatarScale by animateFloatAsState(
        targetValue = if (isAvatarPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "avatar_press_scale"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            // Elegant "Professional Polish" Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // App Icon Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PurplePrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Lock In",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (!essentialGranted) {
                                        isPermissionDialogOpen = true
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (essentialGranted) EmeraldSuccess else AmberWarning)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (essentialGranted) "Guardian Active" else "Permissions Setup Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (essentialGranted) TextSecondary else AmberWarning,
                                    fontWeight = if (essentialGranted) FontWeight.Normal else FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Interactive User Profile Avatar Circle (e.g., "AB" for Anvin Biju)
                    Box(
                        modifier = Modifier
                            .scale(avatarScale)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.8.dp, avatarColor, CircleShape)
                            .clickable(
                                interactionSource = avatarInteractionSource,
                                indication = null,
                                onClick = { isProfileDialogOpen = true }
                            )
                            .testTag("user_profile_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.initials,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = avatarColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DarkBorder)
                )
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    AppTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurplePrimaryDark,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Super fast and fluid tab transitions (160ms tween - zero lag)
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 160),
                        initialOffsetX = { fullWidth -> if (isForward) fullWidth / 5 else -fullWidth / 5 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 160))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(durationMillis = 160),
                        targetOffsetX = { fullWidth -> if (isForward) -fullWidth / 5 else fullWidth / 5 }
                    ) + fadeOut(animationSpec = tween(durationMillis = 160)))
                },
                label = "FastTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    AppTab.HABITS -> HabitsScreen(viewModel = habitViewModel)
                    AppTab.BLOCKER -> AppBlockerScreen(viewModel = blockerViewModel)
                    AppTab.SLEEP -> SleepScreen(viewModel = sleepViewModel)
                    AppTab.STATS -> FocusStatsScreen(viewModel = statsViewModel)
                }
            }
        }
    }

    // First-Time Launch & Permissions Onboarding Setup Dialog
    if (isPermissionDialogOpen) {
        FirstTimePermissionDialog(
            onDismiss = {
                profileRepo.setPermissionOnboardingCompleted(true)
                profileRepo.markFirstLaunchComplete()
                isPermissionDialogOpen = false
            },
            onCompleted = {
                profileRepo.setPermissionOnboardingCompleted(true)
                profileRepo.markFirstLaunchComplete()
                isPermissionDialogOpen = false
                refreshAllPermissions()
            }
        )
    }

    // Profile Settings & Name Change Dialog
    if (isProfileDialogOpen) {
        UserProfileDialog(
            currentProfile = userProfile,
            onDismiss = { isProfileDialogOpen = false },
            onSave = { name, motto, colorHex ->
                profileRepo.updateProfile(name, motto, colorHex)
                isProfileDialogOpen = false
            }
        )
    }
}
