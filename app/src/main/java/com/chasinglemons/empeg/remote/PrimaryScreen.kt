package com.chasinglemons.empeg.remote

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.model.ScreenType
import com.chasinglemons.empeg.navigation.Screen
import com.chasinglemons.empeg.playlist.PlaylistScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: PrimaryScreenViewModel
) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    val empegIp = viewModel.empegIp.collectAsStateWithLifecycle()
    val lensColor = viewModel.lensColor.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

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
                    IconButton(
                        onClick = {
                            overflowMenuExpanded = !overflowMenuExpanded
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                        )
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = {
                                overflowMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overflow_menu_item_send_message)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    // TODO: Add func
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overflow_menu_item_discovery)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    navController.navigate(Screen.Discovery.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overflow_menu_item_settings)) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    // TODO: Add func
                                }
                            )
                        }
                    }
                }
            )
        },
        content = { padding ->
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> {

                    val pagerScreens = listOf(
                        ScreenType.RemoteScreen,
                        ScreenType.PlaylistScreen
                    )
                    val pagerState = rememberPagerState(pageCount = { pagerScreens.size })

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f) // Ensure Pager takes remaining space
                        ) { pageIndex ->
                            when (pagerScreens[pageIndex]) {
                                ScreenType.RemoteScreen -> {
                                    RemoteScreen(
                                        modifier = Modifier
                                            .padding(bottom = 10.dp),
                                        empegIp = empegIp.value,
                                        buttonPress = {
                                            coroutineScope.launch {
                                                viewModel.sendCommand(it)
                                            }
                                        }
                                    )
                                }

                                ScreenType.PlaylistScreen -> {
                                    PlaylistScreen(
                                        playlistClick = { }
                                    )
                                }
                            }
                        }

                        // TabRow for page indication and navigation
                        TabRow(
                            modifier = Modifier
//                                .padding(bottom = 30.dp)
                                .height(30.dp),
                            selectedTabIndex = pagerState.currentPage,
                            indicator = { tabPositions ->
                                if (pagerState.currentPage < tabPositions.size) {
                                    SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                        12.dp, Color(0xFF00BFFF)
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
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                WindowWidthSizeClass.Medium -> Text(color = Color.Red, text = "REMOTE medium")
                WindowWidthSizeClass.Expanded -> Text(color = Color.Red, text = "REMOTE expanded")
            }
        }
    )
}