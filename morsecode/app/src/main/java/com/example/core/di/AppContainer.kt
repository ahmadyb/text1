package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.core.database.MorsecodeDatabase
import com.example.core.preferences.PreferencesManager
import com.example.core.storage.StorageManager

interface AppContainer {
    val database: MorsecodeDatabase
    val preferencesManager: PreferencesManager
    val storageManager: StorageManager
    val playerManager: com.example.core.media.Media3PlayerManager
    val durableEngine: com.example.core.transfer.DurableTransferEngine
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: MorsecodeDatabase by lazy {
        Room.databaseBuilder(
            context,
            MorsecodeDatabase::class.java,
            "morsecode.db"
        ).fallbackToDestructiveMigration().build()
    }

    override val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    override val storageManager: StorageManager by lazy {
        StorageManager(context)
    }

    override val playerManager: com.example.core.media.Media3PlayerManager by lazy {
        com.example.core.media.Media3PlayerManager(context)
    }

    override val durableEngine: com.example.core.transfer.DurableTransferEngine by lazy {
        com.example.core.transfer.DurableTransferEngine(context, database)
    }
}
