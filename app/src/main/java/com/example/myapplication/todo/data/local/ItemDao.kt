package com.example.myapplication.todo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.todo.data.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM Items")
    fun getAll(): Flow<List<Item>>

    @Query("SELECT * FROM Items WHERE _id = :id LIMIT 1")
    suspend fun getById(id: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<Item>)

    @Update
    suspend fun update(item: Item): Int

    @Delete
    suspend fun delete(item: Item)

    @Query("DELETE FROM Items WHERE _id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM Items")
    suspend fun deleteAll()
}

