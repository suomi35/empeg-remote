package com.chasinglemons.empeg.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    navController: NavController,
    viewModel: DiscoveryViewModel
) {

    var manualIpAddress by remember { mutableStateOf(String()) }
    val lazyListState = rememberLazyListState()
    val discoveryListState = viewModel.discoveryFlow.collectAsStateWithLifecycle()
    val showProgressIndicator = viewModel.showProgressIndicator.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.discovery_title)) },
                navigationIcon = {
                    if (viewModel.getCurrentlyHomedPlayerIp().isNotBlank()) {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.description_discovery_back_icon)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.searchForEmpegs() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.description_empeg_discovery_icon)
                        )
                    }

                    IconButton(onClick = { navController.navigate(Screen.Primary.route) }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.description_show_settings_icon)
                        )
                    }
                }
            )
        },
        content = { padding ->

            Box(Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        text = when (viewModel.getCurrentlyHomedPlayerIp().isBlank()) {
                            true -> "Manual IP entry"
                            false -> "Currently controlling empeg at:"
                        }
                    )

                    Row(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            modifier = Modifier
                                .weight(1f),
                            placeholder = {
                                Text(stringResource(R.string.discovery_ip_address_placeholder))
                            },
                            // Uri (not Decimal): the player may be "host:port"
                            // for a simulator on a non-80 port, and the decimal
                            // keypad has no colon to type it with.
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            value = manualIpAddress,
                            onValueChange = {
                                manualIpAddress = it // TODO: IP address validation?!
                            },
                        )

                        TextButton(
                            onClick = {
                                viewModel.setPlayer(manualIpAddress)
                                navController.popBackStack()
                            }
                        ) {
                            Text(text = stringResource(R.string.discovery_ip_address_ok))
                        }
                    }

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        textAlign = TextAlign.Center,
                        text = "Discovered players"
                    )

                    LazyColumn(
//        contentPadding = innerPadding, // TODO: Do we need this?
                        state = lazyListState
                    ) {
                        items(
                            items = discoveryListState.value,
                            key = { empeg -> empeg.ip },
                            itemContent = { item ->
                                Row(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setPlayer(item.ip)
                                            navController.popBackStack()
                                        }
                                ) {
                                    Column {
                                        Text(item.name)
                                        Text("(${item.ip})")
                                    }
                                }
                            }
                        )
                    }
                }

                if (showProgressIndicator.value) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .wrapContentSize()
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                manualIpAddress = viewModel.getCurrentlyHomedPlayerIp() // TODO: Should this be run through the viewmodel as a stateflow?
                viewModel.searchForEmpegs()
            }
        }
    )
}