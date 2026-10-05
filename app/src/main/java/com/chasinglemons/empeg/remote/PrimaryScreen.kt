package com.chasinglemons.empeg.remote

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistState
import com.chasinglemons.empeg.model.ScreenType
import com.chasinglemons.empeg.model.SwipeAction
import com.chasinglemons.empeg.navigation.navigateToConnect
import com.chasinglemons.empeg.notification.PlayerNotification
import com.chasinglemons.empeg.playlist.PlaylistScreen
import com.chasinglemons.empeg.ui.components.FasciaScreen
import com.chasinglemons.empeg.ui.components.KeepScreenOn
import com.chasinglemons.empeg.ui.components.LoadingAnimation
import com.chasinglemons.empeg.ui.components.NetworkErrorDialog
import com.chasinglemons.empeg.ui.components.PlaylistUnreachable
import com.chasinglemons.empeg.ui.components.SendMessageDialog
import com.chasinglemons.empeg.ui.components.SettingsDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: PrimaryScreenViewModel
) {
    val orientation = LocalConfiguration.current.orientation
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()
    val empegIp by viewModel.empegIp.collectAsStateWithLifecycle()
    val lensColor by viewModel.lensColor.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val playlistHistory by viewModel.playlistHistory.collectAsStateWithLifecycle()
    val persistentNotification by viewModel.persistentNotification.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val vibrate by viewModel.vibrate.collectAsStateWithLifecycle()
    val discoveryTimeout by viewModel.discoveryTimeout.collectAsStateWithLifecycle()
    val swipeAction by viewModel.swipeAction.collectAsStateWithLifecycle()
    val screenRefreshRate by viewModel.screenRefreshRate.collectAsStateWithLifecycle()
    val usePixelFont by viewModel.usePixelFont.collectAsStateWithLifecycle()
    val showDisplay by viewModel.showDisplay.collectAsStateWithLifecycle()
    val useKeyboard by viewModel.useKeyboard.collectAsStateWithLifecycle()
    val showDisplayBoard by viewModel.showDisplayBoard.collectAsStateWithLifecycle()
    val commandError by viewModel.commandError.collectAsStateWithLifecycle()

    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showSendMessageDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            PlayerNotification.show(context, empegIp)
        } else {
            viewModel.updatePersistentNotification(false)
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshFromPreferences()
        }
    }

    LaunchedEffect(persistentNotification, empegIp) {
        if (!persistentNotification) {
            PlayerNotification.cancel(context)
            return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                PlayerNotification.show(context, empegIp)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            PlayerNotification.show(context, empegIp)
        }
    }

    KeepScreenOn(keepScreenOn = keepScreenOn)

    val onRemoteButton: (String) -> Unit = { command ->
        if (vibrate) {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        }
        viewModel.sendCommand(command)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = stringResource(R.string.description_empeg_remote_icon)
                    )
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = {
                                overflowMenuExpanded = !overflowMenuExpanded
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = {
                                overflowMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overflow_menu_item_connect)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    navController.navigateToConnect()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overflow_menu_item_send_message)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showSendMessageDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overflow_menu_item_settings)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showSettingsDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        content = { padding ->
            when (orientation) {
                Configuration.ORIENTATION_PORTRAIT,
                Configuration.ORIENTATION_UNDEFINED -> {
                    when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact -> {
                            CompactRemotePlaylistPager(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                empegIp = empegIp,
                                lensColor = lensColor,
                                screenRefreshRate = screenRefreshRate,
                                useKeyboard = useKeyboard,
                                showDisplay = showDisplay,
                                swipeAction = SwipeAction.fromString(swipeAction),
                                playlistState = playlistState,
                                playlist = playlist,
                                playlistHistorySize = playlistHistory.size,
                                usePixelFont = usePixelFont,
                                onRemoteButton = onRemoteButton,
                                updateLensColor = { viewModel.updateLensColor(it) },
                                playlistClick = { viewModel.fetchPlaylist(it) },
                                tuneClick = { viewModel.playTune(it) },
                                overflowAction = { viewModel.playTune(it) },
                                downloadTune = { viewModel.downloadTune(it) },
                                backPressed = { viewModel.navigateBackInPlaylistHistory() },
                                onRetry = { viewModel.fetchPlaylist() },
                                onHaptic = {
                                    if (vibrate) {
                                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    }
                                }
                            )
                        }

                        WindowWidthSizeClass.Medium,
                        WindowWidthSizeClass.Expanded -> {
                            SplitRemoteAndPlaylists(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                empegIp = empegIp,
                                lensColor = lensColor,
                                screenRefreshRate = screenRefreshRate,
                                useKeyboard = useKeyboard,
                                showDisplay = showDisplay,
                                playlistState = playlistState,
                                playlist = playlist,
                                playlistHistorySize = playlistHistory.size,
                                usePixelFont = usePixelFont,
                                onRemoteButton = onRemoteButton,
                                updateLensColor = { viewModel.updateLensColor(it) },
                                playlistClick = { viewModel.fetchPlaylist(it) },
                                tuneClick = { viewModel.playTune(it) },
                                overflowAction = { viewModel.playTune(it) },
                                downloadTune = { viewModel.downloadTune(it) },
                                backPressed = { viewModel.navigateBackInPlaylistHistory() },
                                onRetry = { viewModel.fetchPlaylist() },
                                onHaptic = {
                                    if (vibrate) {
                                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    }
                                }
                            )
                        }
                    }
                }

                Configuration.ORIENTATION_LANDSCAPE -> {
                    when (windowSizeClass.heightSizeClass) {
                        WindowHeightSizeClass.Compact -> {
                            FasciaScreen(
                                modifier = Modifier.padding(padding),
                                empegIp = empegIp,
                                lensColor = lensColor,
                                refreshDelay = screenRefreshRate.toLong(),
                                showDisplayBoard = showDisplayBoard,
                                onClick = onRemoteButton
                            )
                        }

                        WindowHeightSizeClass.Medium,
                        WindowHeightSizeClass.Expanded -> {
                            SplitRemoteAndPlaylists(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                empegIp = empegIp,
                                lensColor = lensColor,
                                screenRefreshRate = screenRefreshRate,
                                useKeyboard = useKeyboard,
                                showDisplay = showDisplay,
                                playlistState = playlistState,
                                playlist = playlist,
                                playlistHistorySize = playlistHistory.size,
                                usePixelFont = usePixelFont,
                                onRemoteButton = onRemoteButton,
                                updateLensColor = { viewModel.updateLensColor(it) },
                                playlistClick = { viewModel.fetchPlaylist(it) },
                                tuneClick = { viewModel.playTune(it) },
                                overflowAction = { viewModel.playTune(it) },
                                downloadTune = { viewModel.downloadTune(it) },
                                backPressed = { viewModel.navigateBackInPlaylistHistory() },
                                onRetry = { viewModel.fetchPlaylist() },
                                onHaptic = {
                                    if (vibrate) {
                                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showSendMessageDialog) {
                SendMessageDialog(
                    onSendMessage = {
                        viewModel.sendMessage(it)
                    },
                    onDismiss = {
                        showSendMessageDialog = false
                    }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false },
                    lensColor = lensColor,
                    updateLensColor = { viewModel.updateLensColor(it) },
                    persistentNotification = persistentNotification,
                    updatePersistentNotification = { viewModel.updatePersistentNotification(it) },
                    keepScreenOn = keepScreenOn,
                    updateKeepScreenOn = { viewModel.updateKeepScreenOn(it) },
                    vibrate = vibrate,
                    updateVibrate = { viewModel.updateVibrate(it) },
                    discoveryTimeout = discoveryTimeout,
                    updateDiscoveryTimeout = { viewModel.updateDiscoveryTimeout(it) },
                    swipeAction = SwipeAction.fromString(swipeAction),
                    updateSwipeAction = { viewModel.updateSwipeAction(it) },
                    screenRefreshRate = screenRefreshRate,
                    updateScreenRefreshRate = { viewModel.updateScreenRefreshRate(it) },
                    usePixelFont = usePixelFont,
                    updateUsePixelFont = { viewModel.updateUsePixelFont(it) },
                    showDisplay = showDisplay,
                    updateShowDisplay = { viewModel.updateShowDisplay(it) },
                    useKeyboard = useKeyboard,
                    updateUseKeyboard = { viewModel.updateUseKeyboard(it) },
                    showDisplayBoard = showDisplayBoard,
                    updateShowDisplayBoard = { viewModel.updateShowDisplayBoard(it) }
                )
            }

            if (commandError != null) {
                NetworkErrorDialog(
                    onDismiss = { viewModel.consumeCommandError() }
                )
            }
        }
    )
}

@Composable
private fun CompactRemotePlaylistPager(
    modifier: Modifier,
    empegIp: String,
    lensColor: Color,
    screenRefreshRate: Int,
    useKeyboard: Boolean,
    showDisplay: Boolean,
    swipeAction: SwipeAction,
    playlistState: PlaylistState,
    playlist: SnapshotStateList<Playlist>,
    playlistHistorySize: Int,
    usePixelFont: Boolean,
    onRemoteButton: (String) -> Unit,
    updateLensColor: (Color) -> Unit,
    playlistClick: (String) -> Unit,
    tuneClick: (String) -> Unit,
    overflowAction: (String) -> Unit,
    downloadTune: (Playlist) -> Unit,
    backPressed: () -> Unit,
    onRetry: () -> Unit,
    onHaptic: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerScreens = listOf(ScreenType.RemoteScreen, ScreenType.PlaylistScreen)
    val pagerState = rememberPagerState(pageCount = { pagerScreens.size })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = swipeAction != SwipeAction.GESTURES,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) { pageIndex ->
            when (pagerScreens[pageIndex]) {
                ScreenType.RemoteScreen -> {
                    RemoteScreen(
                        modifier = Modifier.padding(bottom = 10.dp),
                        empegIp = empegIp,
                        buttonPress = onRemoteButton,
                        lensColor = lensColor,
                        updateLensColor = updateLensColor,
                        screenRefreshRate = screenRefreshRate,
                        useKeyboard = useKeyboard,
                        showDisplay = showDisplay
                    )
                }

                ScreenType.PlaylistScreen -> {
                    PlaylistPane(
                        playlistState = playlistState,
                        playlist = playlist,
                        playlistHistorySize = playlistHistorySize,
                        lensColor = lensColor,
                        usePixelFont = usePixelFont,
                        playlistClick = playlistClick,
                        tuneClick = tuneClick,
                        overflowAction = overflowAction,
                        downloadTune = downloadTune,
                        backPressed = backPressed,
                        onRetry = onRetry,
                        onHaptic = onHaptic
                    )
                }
            }
        }

        TabRow(
            modifier = Modifier.height(30.dp),
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        12.dp,
                        lensColor
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            pagerScreens.forEachIndexed { index, pageType ->
                val title = when (pageType) {
                    ScreenType.RemoteScreen -> "Remote"
                    ScreenType.PlaylistScreen -> "Playlists"
                }
                Tab(
                    text = { Text(title) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    selectedContentColor = lensColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SplitRemoteAndPlaylists(
    modifier: Modifier,
    empegIp: String,
    lensColor: Color,
    screenRefreshRate: Int,
    useKeyboard: Boolean,
    showDisplay: Boolean,
    playlistState: PlaylistState,
    playlist: SnapshotStateList<Playlist>,
    playlistHistorySize: Int,
    usePixelFont: Boolean,
    onRemoteButton: (String) -> Unit,
    updateLensColor: (Color) -> Unit,
    playlistClick: (String) -> Unit,
    tuneClick: (String) -> Unit,
    overflowAction: (String) -> Unit,
    downloadTune: (Playlist) -> Unit,
    backPressed: () -> Unit,
    onRetry: () -> Unit,
    onHaptic: () -> Unit
) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(.36F)) {
            RemoteScreen(
                modifier = Modifier.padding(bottom = 10.dp),
                empegIp = empegIp,
                buttonPress = onRemoteButton,
                lensColor = lensColor,
                updateLensColor = updateLensColor,
                screenRefreshRate = screenRefreshRate,
                useKeyboard = useKeyboard,
                showDisplay = showDisplay
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(.64F)
        ) {
            PlaylistPane(
                playlistState = playlistState,
                playlist = playlist,
                playlistHistorySize = playlistHistorySize,
                lensColor = lensColor,
                usePixelFont = usePixelFont,
                playlistClick = playlistClick,
                tuneClick = tuneClick,
                overflowAction = overflowAction,
                downloadTune = downloadTune,
                backPressed = backPressed,
                onRetry = onRetry,
                onHaptic = onHaptic
            )
        }
    }
}

@Composable
private fun PlaylistPane(
    playlistState: PlaylistState,
    playlist: SnapshotStateList<Playlist>,
    playlistHistorySize: Int,
    lensColor: Color,
    usePixelFont: Boolean,
    playlistClick: (String) -> Unit,
    tuneClick: (String) -> Unit,
    overflowAction: (String) -> Unit,
    downloadTune: (Playlist) -> Unit,
    backPressed: () -> Unit,
    onRetry: () -> Unit,
    onHaptic: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (playlistState) {
            is PlaylistState.Loaded -> {
                PlaylistScreen(
                    playlistClick = {
                        onHaptic()
                        playlistClick(it)
                    },
                    tuneClick = {
                        onHaptic()
                        tuneClick(it)
                    },
                    overflowAction = {
                        onHaptic()
                        overflowAction(it)
                    },
                    downloadTune = {
                        onHaptic()
                        downloadTune(it)
                    },
                    playlist = playlist,
                    handleBackButton = playlistHistorySize > 1,
                    backPressed = {
                        onHaptic()
                        backPressed()
                    },
                    lensColor = lensColor,
                    usePixelFont = usePixelFont
                )
            }

            is PlaylistState.Error -> {
                PlaylistUnreachable(onRetry = onRetry)
            }

            is PlaylistState.Loading -> {
                LoadingAnimation(
                    color = lensColor,
                    imageSize = 60.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
