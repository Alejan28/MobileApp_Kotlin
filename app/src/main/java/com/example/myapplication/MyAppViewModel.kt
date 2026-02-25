package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.core.TAG
import com.example.myapplication.core.data.UserPreferences
import com.example.myapplication.core.data.UserPreferencesRepository
import com.example.myapplication.todo.data.ItemRepository
import kotlinx.coroutines.launch

class MyAppViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val itemRepository: ItemRepository
) :
    ViewModel() {

    init {
        Log.d(TAG, "init")
    }

    fun logout() {
        viewModelScope.launch {
            itemRepository.deleteAll()
            userPreferencesRepository.save(UserPreferences())
        }
    }

    fun setToken(token: String) {
        itemRepository.setToken(token)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)
                MyAppViewModel(
                    app.container.userPreferencesRepository,
                    app.container.itemRepository
                )
            }
        }
    }
}

