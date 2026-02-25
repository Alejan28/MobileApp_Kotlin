package com.example.myapplication

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.auth.LoginScreen
import com.example.myapplication.core.data.UserPreferences
import com.example.myapplication.core.data.remote.Api
import com.example.myapplication.core.ui.UserPreferencesViewModel
import com.example.myapplication.core.util.connectivityState
import com.example.myapplication.todo.ui.item.ItemScreen
import com.example.myapplication.todo.ui.items.ItemsScreen

val itemsRoute = "items"
val authRoute = "auth"

@Composable
fun MyAppNavHost() {
    val navController = rememberNavController()
    val myAppViewModel = viewModel<MyAppViewModel>(factory = MyAppViewModel.Factory)
    val userPreferencesViewModel = viewModel<UserPreferencesViewModel>(factory = UserPreferencesViewModel.Factory)

    val userPreferencesUiState by userPreferencesViewModel.uiState.collectAsStateWithLifecycle(initialValue = UserPreferences())

    val onCloseItem: () -> Unit = { navController.popBackStack() }

    val isNetworkAvailable by connectivityState()
    var wasNetworkAvailable by remember { mutableStateOf(isNetworkAvailable) }
    val context = LocalContext.current

    LaunchedEffect(isNetworkAvailable) {
        if (!wasNetworkAvailable && isNetworkAvailable) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, "default")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Back Online")
                .setContentText("Your device is back online. Your changes will now be synced.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(3, builder.build()) // Use a new unique ID
                }
            }
        }
        wasNetworkAvailable = isNetworkAvailable
    }

    NavHost(navController = navController, startDestination = authRoute) {
        composable(itemsRoute) {
            ItemsScreen(
                onItemClick = { id -> navController.navigate("$itemsRoute/$id") },
                onAddItem = { navController.navigate("$itemsRoute-new") },
                onLogout = {
                    myAppViewModel.logout()
                    Api.tokenInterceptor.token = null
                    navController.navigate(authRoute) { popUpTo(0) }
                }
            )
        }
        composable(
            "$itemsRoute/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            ItemScreen(itemId = it.arguments?.getString("id"), onClose = onCloseItem)
        }
        composable("$itemsRoute-new") {
            ItemScreen(itemId = null, onClose = onCloseItem)
        }
        composable(authRoute) {
            LoginScreen(onClose = { navController.navigate(itemsRoute) })
        }
    }

    LaunchedEffect(userPreferencesUiState.token) {
        if (userPreferencesUiState.token.isNotEmpty()) {
            Api.tokenInterceptor.token = userPreferencesUiState.token
            myAppViewModel.setToken(userPreferencesUiState.token)
            navController.navigate(itemsRoute) { popUpTo(0) }
        }
    }
}
