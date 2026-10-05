package com.chasinglemons.empeg.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
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
import com.chasinglemons.empeg.navigation.navigateToPrimaryAfterConnect
import com.chasinglemons.empeg.ui.components.LoadingAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    navController: NavController,
    viewModel: ConnectViewModel,
    empegIp: String
) {

    var manualIpAddress by remember { mutableStateOf(empegIp) }
    val lazyListState = rememberLazyListState()
    val discoveryListState = viewModel.discoveryFlow.collectAsStateWithLifecycle()
    val showProgressIndicator = viewModel.showProgressIndicator.collectAsStateWithLifecycle()
    val lensColor = viewModel.lensColor.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connect_title)) },
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
                                Text(stringResource(R.string.connect_ip_address_placeholder))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            value = manualIpAddress,
                            onValueChange = {
                                manualIpAddress = it // TODO: IP address validation?!
                            },
                        )

                        TextButton(
                            onClick = {
                                viewModel.setPlayer(manualIpAddress)
                                navController.navigateToPrimaryAfterConnect()
                            }
                        ) {
                            Text(text = stringResource(R.string.ok))
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
                                            navController.navigateToPrimaryAfterConnect()
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
                        LoadingAnimation(
                            color = lensColor.value,
                            imageSize = 60.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.searchForEmpegs()
            }
        }
    )
}