package com.chasinglemons.empeg.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.chasinglemons.empeg.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(scrollBehavior: TopAppBarScrollBehavior, visible: Boolean) {

    AnimatedVisibility(visible = visible) {
        CenterAlignedTopAppBar(
            title = { stringResource(R.string.app_name) },
            navigationIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = stringResource(R.string.description_empeg_remote_icon)
                )
            },
            scrollBehavior = scrollBehavior
        )
    }
}