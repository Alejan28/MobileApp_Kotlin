package com.example.myapplication.todo.data

import android.util.Log
import com.example.myapplication.core.TAG
import com.example.myapplication.core.data.remote.Api
import com.example.myapplication.todo.data.local.ItemDao
import com.example.myapplication.todo.data.remote.ItemEvent
import com.example.myapplication.todo.data.remote.ItemService
import com.example.myapplication.todo.data.remote.ItemWsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class ItemRepository(
    private val itemService: ItemService,
    private val itemWsClient: ItemWsClient,
    private val itemDao: ItemDao
) {
    val itemStream: Flow<List<Item>> = itemDao.getAll()

    init {
        Log.d(TAG, "init")
    }

    private fun getBearerToken() = "Bearer ${Api.tokenInterceptor.token}"

    suspend fun refresh() {
        Log.d(TAG, "refresh started")
        try {
            val itemsFromServer = itemService.find(authorization = getBearerToken())
            itemDao.deleteAll()
            itemsFromServer.forEach { itemDao.insert(it) }
            Log.d(TAG, "refresh succeeded")
        } catch (e: Exception) {
            Log.w(TAG, "refresh failed", e)
        }
    }

    suspend fun saveOrUpdateOffline(item: Item): Item {
        Log.d(TAG, "saveOrUpdateOffline $item...")
        val itemToSave = if (item._id.isEmpty()) {
            Log.d(TAG, "saveOrUpdateOffline $item...")
            item.copy(_id = UUID.randomUUID().toString(), dirty = true)
        } else {
            item.copy(dirty = true)
        }
        itemDao.insert(itemToSave)
        Log.d(TAG, "item saved to local db: $itemToSave")
        return itemToSave
    }

    suspend fun sync() {
        Log.d(TAG, "sync started")
        try {
            val items = itemDao.getAll().first()
            val dirtyItems = items.filter { it.dirty }
            Log.d(TAG, ">>> sync found ${dirtyItems.size} dirty items")

            for (item in dirtyItems) {
                try {
                    if (item.version == 0) {
                        Log.d(TAG, "sync CREATING item on server: $item")
                        val itemToSend = item.copy(_id = "")
                        val createdItem = itemService.create(item = itemToSend, authorization = getBearerToken())
                        Log.d(TAG, "sync DELETING old local item: ${item._id}")
                        itemDao.delete(item)
                        val finalItem = createdItem.copy(version = 1, dirty = false)
                        Log.d(TAG, "sync INSERTING new item from server: $finalItem")
                        itemDao.insert(finalItem)
                    } else {
                        Log.d(TAG, "sync UPDATING item on server: $item")
                        val updatedItem = itemService.update(itemId = item._id, item = item, authorization = getBearerToken())
                        Log.d(TAG, "sync UPDATING local item: $updatedItem")
                        itemDao.update(updatedItem.copy(dirty = false))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "sync failed for item $item", e)
                }
            }
            Log.d(TAG, "sync finished")
        } catch (e: Exception) {
            Log.e(TAG, "sync failed", e)
        }
    }

    suspend fun openWsClient() {
        Log.d(TAG, "openWsClient")
        withContext(Dispatchers.IO) {
            getItemEvents().collect {
                Log.d(TAG, "Item event collected $it")
                if (it.isSuccess) {
                    val itemEvent = it.getOrNull();
                    when (itemEvent?.type) {
                        "created" -> itemDao.insert(itemEvent.payload)
                        "updated" -> itemDao.update(itemEvent.payload)
                        "deleted" -> itemDao.delete(itemEvent.payload)
                    }
                }
            }
        }
    }

    suspend fun closeWsClient() {
        Log.d(TAG, "closeWsClient")
        withContext(Dispatchers.IO) {
            itemWsClient.closeSocket()
        }
    }

    private suspend fun getItemEvents(): Flow<Result<ItemEvent>> = callbackFlow {
        Log.d(TAG, "getItemEvents started")
        itemWsClient.openSocket(
            onEvent = {
                Log.d(TAG, "onEvent $it")
                if (it != null) {
                    trySend(Result.success(it))
                }
            },
            onClosed = { close() },
            onFailure = { close() });
        awaitClose { itemWsClient.closeSocket() }
    }

    suspend fun deleteAll() {
        itemDao.deleteAll()
    }

    fun setToken(token: String) {
        itemWsClient.authorize(token)
    }
}
