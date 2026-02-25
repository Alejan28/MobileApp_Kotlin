package com.example.myapplication.todo.ui.items

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.MyApplication
import com.example.myapplication.core.TAG
import com.example.myapplication.todo.data.Item
import com.example.myapplication.todo.data.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ItemsViewModel(private val itemRepository: ItemRepository) : ViewModel() {
    // This stream already provides Flow<List<Item>> and observes database changes
    val uiState: Flow<List<Item>> = itemRepository.itemStream

    init {
        Log.d(TAG, "init")
        loadItems()
    }

    fun loadItems() {
        Log.d(TAG, "loadItems...")
        // This refresh performs the network fetch and updates the local Room database,
        // which then automatically updates the uiState Flow above.
        viewModelScope.launch {
            itemRepository.refresh()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)
                ItemsViewModel(app.container.itemRepository)
            }
        }
    }
}