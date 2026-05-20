package com.example.inventory.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.inventory.InventoryApplication
import com.example.inventory.ui.home.HomeViewModel

/**
 * Provides Factory to create instance of ViewModel for the entire Inventory app
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            // HomeViewModelにリポジトリとアプリケーションコンテキストを渡す
            HomeViewModel(
                schedulesRepository = inventoryApplication().container.schedulesRepository,
                application = inventoryApplication() // ★ここを追記！
            )
        }
    }
}


/**
 * Extension function to get the InventoryApplication object.
 */
fun CreationExtras.inventoryApplication(): InventoryApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as InventoryApplication)
