package com.example.virtualpetkmp.data

import com.example.virtualpetkmp.db.VirtualPetDatabase

expect class DatabaseFactory {
    fun createDatabase(): VirtualPetDatabase
}
