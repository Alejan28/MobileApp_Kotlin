package com.example.myapplication.todo.ui.item

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.MyApplication
import com.example.myapplication.core.Result
import com.example.myapplication.core.TAG
import com.example.myapplication.core.util.LocationManager
import com.example.myapplication.todo.data.Item
import com.example.myapplication.todo.data.ItemRepository
import com.example.myapplication.todo.data.worker.SyncWorker
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ItemUiState(
    val itemId: String? = null,
    val item: Item = Item(),
    var loadResult: Result<Item>? = null,
    var submitResult: Result<Item>? = null,
)

class ItemViewModel(
    private val itemId: String?,
    private val itemRepository: ItemRepository,
    private val workManager: WorkManager,
    private val locationManager: LocationManager
) : ViewModel() {

    var uiState: ItemUiState by mutableStateOf(ItemUiState(loadResult = Result.Loading))
        private set

    init {
        Log.d(TAG, "init")
        if (itemId != null) {
            loadItem()
        } else {
            uiState = uiState.copy(loadResult = Result.Success(Item()))
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            itemRepository.itemStream
                .map { items -> items.find { it._id == itemId } ?: Item() }
                .collect { item ->
                    if (uiState.loadResult is Result.Loading || item != uiState.item) {
                        uiState = uiState.copy(item = item, loadResult = Result.Success(item))
                    }
                }
        }
    }

    fun saveOrUpdateItem(title: String, artist: String, noTracks: Int, releaseDate: String, lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            Log.d(TAG, "saveOrUpdateItem...");
            uiState = uiState.copy(submitResult = Result.Loading)
            try {
                // If coordinates aren't provided (e.g. for a new item), try to get them automatically
                val finalLat: Double
                val finalLon: Double
                
                if (lat != null && lon != null) {
                    finalLat = lat
                    finalLon = lon
                } else if (itemId == null) {
                    val location = locationManager.getCurrentLocation()
                    finalLat = location?.latitude ?: 0.0
                    finalLon = location?.longitude ?: 0.0
                } else {
                    finalLat = uiState.item.lat
                    finalLon = uiState.item.lon
                }
                
                val item = uiState.item.copy(
                    title = title,
                    artist = artist,
                    noTracks = noTracks,
                    releaseDate = releaseDate,
                    lat = finalLat,
                    lon = finalLon
                )
                
                val savedItem = itemRepository.saveOrUpdateOffline(item)
                Log.d(TAG, "saveOrUpdateItem succeeded")
                uiState = uiState.copy(submitResult = Result.Success(savedItem))
                scheduleSync()
            } catch (e: Exception) {
                Log.d(TAG, "saveOrUpdateItem failed")
                uiState = uiState.copy(submitResult = Result.Error(e))
            }
        }
    }

    private fun scheduleSync() {
        Log.d(TAG, "scheduleSync")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            "syncItems",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        fun Factory(itemId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)
                ItemViewModel(
                    itemId,
                    app.container.itemRepository,
                    WorkManager.getInstance(app.applicationContext),
                    LocationManager(app.applicationContext)
                )
            }
        }
    }
}
