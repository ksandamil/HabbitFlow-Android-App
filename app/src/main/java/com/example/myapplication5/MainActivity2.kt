package com.example.myapplication5

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment


class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // Use your bottom-nav layout

        // Load default fragment (Habits)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HabitsFragment())
                .commit()
        }

        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when(item.itemId) {
                R.id.nav_habits -> HabitsFragment()
                R.id.nav_mood -> MoodFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HabitsFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, selectedFragment)
                .commit()

            true
        }
    }
}
