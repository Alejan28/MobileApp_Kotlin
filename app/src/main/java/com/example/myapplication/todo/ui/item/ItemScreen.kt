package com.example.myapplication.todo.ui.item

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.core.Result
import com.example.myapplication.core.util.connectivityState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(itemId: String?, onClose: () -> Unit) {
    val itemViewModel = viewModel<ItemViewModel>(factory = ItemViewModel.Factory(itemId))
    val itemUiState = itemViewModel.uiState
    val isNetworkAvailable by connectivityState()
    var isSaving by remember { mutableStateOf(false) }

    var title by rememberSaveable { mutableStateOf(itemUiState.item.title) }
    var artist by rememberSaveable { mutableStateOf(itemUiState.item.artist) }
    var noTracksString by rememberSaveable { mutableStateOf(itemUiState.item.noTracks.toString()) }
    var releaseDate by rememberSaveable { mutableStateOf(itemUiState.item.releaseDate) }
    

    var pickedLat by rememberSaveable { mutableStateOf(itemUiState.item.lat) }
    var pickedLon by rememberSaveable { mutableStateOf(itemUiState.item.lon) }

    var showDatePicker by remember { mutableStateOf(false) }

    Log.d("ItemScreen", "recompose, title = $title")

    val context = LocalContext.current
    LaunchedEffect(itemUiState.submitResult) {
        if (itemUiState.submitResult is Result.Success) {
            if (itemId == null) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                val builder = NotificationCompat.Builder(context, "default")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("New Item Added")
                    .setContentText("A new item '$title' has been added to your list.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    with(NotificationManagerCompat.from(context)) {
                        notify(2, builder.build())
                    }
                }
            }
            isSaving = false
            onClose()
        }
    }

    var itemInitialized by remember { mutableStateOf(itemId == null) }
    LaunchedEffect(itemId, itemUiState.loadResult) {
        if (itemInitialized) return@LaunchedEffect
        if (itemUiState.loadResult !is Result.Loading) {
            val item = itemUiState.item
            title = item.title
            artist = item.artist
            noTracksString = item.noTracks.toString()
            releaseDate = item.releaseDate
            pickedLat = item.lat
            pickedLon = item.lon
            itemInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.item)) },
                actions = {
                    Button(
                        onClick = {
                            isSaving = true
                            val noTracksInt = noTracksString.toIntOrNull() ?: 0

                            itemViewModel.saveOrUpdateItem(title, artist, noTracksInt, releaseDate, pickedLat, pickedLon)
                        },
                        enabled = !isSaving
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isNetworkAvailable) {
                Text(
                    text = "Network unavailable. Your changes will be saved locally and synced when you are back online.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (itemUiState.loadResult is Result.Loading || itemUiState.submitResult is Result.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))

            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            TextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            TextField(
                value = noTracksString,
                onValueChange = { noTracksString = it },
                label = { Text("No. Tracks") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Release Date: ${if (releaseDate.isBlank()) "Select…" else releaseDate}")
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            // Read selected date from DatePicker
                            // Format as string
                            // Convert epoch to string (example: YYYY-MM-DD)
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    val datePickerState = rememberDatePickerState()
                    DatePicker(state = datePickerState)
                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        val millis = datePickerState.selectedDateMillis ?: return@LaunchedEffect
                        val formatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date(millis))
                        releaseDate = formatted
                    }
                }
            }


            Spacer(Modifier.height(16.dp))
            Text(
                text = if (pickedLat == 0.0 && pickedLon == 0.0) "Pick Location (Tap Map)" else "Selected Location", 
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            
            val cameraPositionState = rememberCameraPositionState {
                val initialPos = if (pickedLat != 0.0) LatLng(pickedLat, pickedLon) else LatLng(46.7712, 23.6236) // Default to Cluj if no pos
                position = CameraPosition.fromLatLngZoom(initialPos, 15f)
            }


            LaunchedEffect(pickedLat, pickedLon) {
                if (pickedLat != 0.0 || pickedLon != 0.0) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(pickedLat, pickedLon), 15f)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        // 📍 UPDATE POSITION: Set local state on tap
                        pickedLat = latLng.latitude
                        pickedLon = latLng.longitude
                    }
                ) {
                    if (pickedLat != 0.0 || pickedLon != 0.0) {
                        Marker(
                            state = MarkerState(position = LatLng(pickedLat, pickedLon)),
                            title = "Picked Location"
                        )
                    }
                }
            }

            if (itemUiState.submitResult is Result.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Failed to submit item - ${(itemUiState.submitResult as Result.Error).exception?.message}",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewItemScreen() {
    ItemScreen(itemId = "0", onClose = {})
}
