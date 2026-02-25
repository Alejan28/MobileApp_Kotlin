package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.auth.data.AuthRepository
import com.example.myapplication.auth.data.remote.AuthDataSource
import com.example.myapplication.core.TAG
import com.example.myapplication.core.data.UserPreferencesRepository
import com.example.myapplication.core.data.remote.Api
import com.example.myapplication.todo.data.ItemRepository
import com.example.myapplication.todo.data.remote.ItemService
import com.example.myapplication.todo.data.remote.ItemWsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

class AppContainer(val context: Context) {
    init {
        Log.d(TAG, "init")
    }

    private val itemService: ItemService = Api.retrofit.create(ItemService::class.java)
    private val itemWsClient: ItemWsClient = ItemWsClient(Api.okHttpClient)
    private val authDataSource: AuthDataSource = AuthDataSource()

    // 🔑 ANR FIX: Database is now nullable and initialized asynchronously
    private var database: MyAppDatabase? = null

    // 🔑 ANR FIX: Expose a suspending function to handle blocking I/O off the Main Thread
    suspend fun initDatabase() {
        if (database == null) {
            database = withContext(Dispatchers.IO) {
                // This is the blocking I/O call
                MyAppDatabase.getDatabase(context)
            }
        }
    }

    // Repositories now rely on the database being initialized before they are first accessed.
    val itemRepository: ItemRepository by lazy {
        // We use !! here, assuming initDatabase() has completed before the ViewModel accesses this.
        ItemRepository(itemService, itemWsClient, database!!.itemDao())
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(authDataSource)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.userPreferencesDataStore)
    }
}