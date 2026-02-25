package com.example.myapplication.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun connectivityState(): State<Boolean> {
    val context = LocalContext.current
    val observer = remember { NetworkConnectivityObserver(context) }

    DisposableEffect(observer) {
        observer.startObserving()
        onDispose {
            observer.stopObserving()
        }
    }
    return observer.networkStatus.collectAsState()
}
