package com.example.myapplication.todo.ui.items

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.core.util.LightSensorObserver
import com.example.myapplication.core.util.ShakeDetector
import com.example.myapplication.core.util.TiltObserver
import com.example.myapplication.core.util.connectivityState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(onItemClick: (id: String?) -> Unit, onAddItem: () -> Unit, onLogout: () -> Unit) {
    Log.d("ItemsScreen", "recompose")
    val itemsViewModel = viewModel<ItemsViewModel>(factory = ItemsViewModel.Factory)
    val itemsUiState by itemsViewModel.uiState.collectAsStateWithLifecycle(
        initialValue = listOf()
    )
    val isNetworkAvailable by connectivityState()
    val context = LocalContext.current

    // 📳 SENSOR: Shake to Refresh
    val shakeDetector = remember {
        ShakeDetector(context) {
            Log.d("ItemsScreen", "Shake detected! Refreshing...")
            itemsViewModel.loadItems()
        }
    }


    val lightSensorObserver = remember { LightSensorObserver(context) }
    val lux by lightSensorObserver.lux.collectAsStateWithLifecycle()


    val tiltObserver = remember { TiltObserver(context) }
    val tilt by tiltObserver.tilt.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        shakeDetector.start()
        lightSensorObserver.start()
        tiltObserver.start()
        onDispose {
            shakeDetector.stop()
            lightSensorObserver.stop()
            tiltObserver.stop()
        }
    }

    val isDark = lux < 10f
    

    val isTilted = kotlin.math.abs(tilt.first) > 3f
    val appBarColor by animateColorAsState(
        targetValue = if (isTilted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "appBarColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = stringResource(id = R.string.items))
                        if (isTilted) {
                            Text(
                                text = "Tilt detected: ${"%.1f".format(tilt.first)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    Button(onClick = onLogout) { Text("Logout") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d("ItemsScreen", "add")
                    onAddItem()
                },
            ) { Icon(Icons.Rounded.Add, "Add") }
        }
    ) { paddingValues ->
        Surface(
            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(paddingValues)
        ) {
            Column {
                AnimatedVisibility(
                    visible = !isNetworkAvailable,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Network unavailable",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                ItemList(
                    itemList = itemsUiState,
                    onItemClick = onItemClick,
                    modifier = Modifier
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewItemsScreen() {
    ItemsScreen(onItemClick = {}, onAddItem = {}, onLogout = {})
}
