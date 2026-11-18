package com.tapmehard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tapmehard.app.ui.screens.AddAutomationScreen
import com.tapmehard.app.ui.screens.HomeScreen
import com.tapmehard.app.ui.theme.TapMeHardTheme
import com.tapmehard.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapMeHardTheme {
                TapMeHardNavGraph()
            }
        }
    }
}

@Composable
fun TapMeHardNavGraph(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onAddAutomation = { navController.navigate("add_automation") }
            )
        }
        composable("add_automation") {
            AddAutomationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
