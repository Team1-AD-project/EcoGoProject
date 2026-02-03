package com.ecogo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ecogo.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        setupVersionToggle()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup Bottom Navigation (will be shown after login)
        val bottomNav = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)
        
        // Hide/Show bottom nav based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, 
                R.id.signupWizardFragment,
                R.id.onboardingFragment -> {
                    bottomNav.visibility = android.view.View.GONE
                }
                else -> {
                    bottomNav.visibility = android.view.View.VISIBLE
                }
            }
        }
    }
    
    /**
     * Developer option: Long-press bottom navigation to toggle between Home V1 and V2
     */
    private fun setupVersionToggle() {
        binding.bottomNavigation.setOnLongClickListener {
            toggleHomeVersion()
            true
        }
    }
    
    private fun toggleHomeVersion() {
        val prefs = getSharedPreferences("ecogo_prefs", MODE_PRIVATE)
        val useV2 = prefs.getBoolean("use_home_v2", false)
        prefs.edit().putBoolean("use_home_v2", !useV2).apply()
        
        val message = if (!useV2) {
            "Switched to Home V2 (with Banner) ✨\nRestart to see changes"
        } else {
            "Switched to Home V1 (original) 📱\nRestart to see changes"
        }
        
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        
        // Optional: Restart activity to apply changes immediately
        // recreate()
    }
    
    /**
     * Get the current home fragment destination based on user preference
     */
    fun getHomeDestination(): Int {
        val prefs = getSharedPreferences("ecogo_prefs", MODE_PRIVATE)
        val useV2 = prefs.getBoolean("use_home_v2", false)
        return if (useV2) R.id.homeFragmentV2 else R.id.homeFragment
    }
}
