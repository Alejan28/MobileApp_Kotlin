package com.example.myapplication.todo.data.remote

import com.example.myapplication.todo.data.Item

data class ItemEvent(val type: String, val payload: Item)
