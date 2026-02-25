package com.example.myapplication.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val _id: String = "",
    val title: String = "",
    val artist: String = "",
    val noTracks: Int = 0,
    val releaseDate: String = "",
    val date: String = "",
    val version: Int = 0,
    @JvmField
    val dirty: Boolean = false,
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
