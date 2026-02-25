package com.example.myapplication.todo.ui.items

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.todo.data.Item

typealias OnItemFn = (id: String?) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemList(itemList: List<Item>, onItemClick: OnItemFn, modifier: Modifier) {
    Log.d("ItemList", "recompose")
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(itemList, key = { _, item -> item._id }) { index, item ->
            // 🎬 ANIMATION: Entrance animation for each item
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = index * 100)) +
                        slideInVertically(
                            animationSpec = tween(durationMillis = 500, delayMillis = index * 100),
                            initialOffsetY = { it / 2 }
                        ),
                modifier = Modifier.animateItem()
            ) {
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.targetValue) {
                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                        onItemClick(item._id)
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.StartToEnd -> Color.Green.copy(alpha = 0.5f)
                            SwipeToDismissBoxValue.EndToStart -> Color.Blue.copy(alpha = 0.5f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                    }
                ) {
                    ItemDetail(item, onItemClick)
                }
            }
        }
    }
}

@Composable
fun ItemDetail(item: Item, onItemClick: OnItemFn) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(targetValue = if (isPressed) 1.05f else 1f, label = "scale")
    val elevation by animateFloatAsState(targetValue = if (isPressed) 8f else 2f, label = "elevation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onItemClick(item._id) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = item.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Artist: ${item.artist}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            if (item.lat != 0.0 || item.lon != 0.0) {
                Text(
                    text = "Location: ${"%.4f".format(item.lat)}, ${"%.4f".format(item.lon)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
