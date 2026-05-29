package com.example.inventory.data

import android.content.Context


interface AppContainer {
    val schedulesRepository: SchedulesRepository
}

class AppDataContainer(private val context: Context) : AppContainer {

    //-- Daoを呼んでいつでも使える状態に --//
    override val schedulesRepository: SchedulesRepository by lazy {
        OfflineSchedulesRepository(
            InventoryDatabase.getDatabase(context).scheduleDao(),
            gameDao = InventoryDatabase.getDatabase(context).gameDao()
        )
    }
}
//a