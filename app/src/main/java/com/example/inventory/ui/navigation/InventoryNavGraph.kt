package com.example.inventory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.inventory.ui.home.HomeDestination
import com.example.inventory.ui.home.HomeScreen


/**
 * アプリケーションのナビゲーショングラフを提供します。
 */
@Composable
fun InventoryNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route
    ) {
        composable(HomeDestination.route) {
            HomeScreen(
                navigateToItemEntry = {},
                navigateToItemUpdate = {}
            )
        }
    }
}