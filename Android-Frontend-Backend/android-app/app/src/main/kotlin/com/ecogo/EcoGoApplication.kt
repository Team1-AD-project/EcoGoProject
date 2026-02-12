package com.ecogo

import android.app.Application
import com.ecogo.mapengine.data.repository.NavigationHistoryRepository
import com.ecogo.repository.EcoGoRepository

/**
 * EcoGo Application class
 * Used for application-level initialization and singleton management
 */
class EcoGoApplication : Application() {
    
    companion object {
        // Global singleton Repository to avoid redundant creation
        lateinit var repository: EcoGoRepository
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Repository singleton
        repository = EcoGoRepository()

        // Initialize TokenManager globally
        com.ecogo.auth.TokenManager.init(this)

        // Initialize NavigationHistoryRepository (requires Context)
        NavigationHistoryRepository.initialize(this)
        
        // Preload critical data (optional, executed on background thread)
        // Thread {
        //     preloadCriticalData()
        // }.start()
    }
    
    /**
     * Preload critical data (optional)
     */
    private fun preloadCriticalData() {
        // Critical data can be preloaded here
        // e.g., user preferences, caches, etc.
    }
}
