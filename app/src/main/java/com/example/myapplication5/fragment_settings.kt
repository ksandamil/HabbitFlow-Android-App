package com.example.myapplication5

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var switchHydration: SwitchMaterial
    private lateinit var switchHabitReminders: SwitchMaterial
    private lateinit var switchMoodReminders: SwitchMaterial
    private lateinit var spinnerInterval: Spinner
    private lateinit var spinnerWaterGoal: Spinner
    private lateinit var btnSaveHydration: MaterialButton

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmManager: AlarmManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request notifications permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireActivity().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        sharedPreferences =
            requireActivity().getSharedPreferences("WellnessPrefs", Context.MODE_PRIVATE)
        alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        initializeViews(view)
        setupSpinners()
        loadSettings()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        switchHydration = view.findViewById(R.id.switchHydration)
        switchHabitReminders = view.findViewById(R.id.switchHabitReminders)
        switchMoodReminders = view.findViewById(R.id.switchMoodReminders)
        spinnerInterval = view.findViewById(R.id.spinnerInterval)
        spinnerWaterGoal = view.findViewById(R.id.spinnerWaterGoal)
        btnSaveHydration = view.findViewById(R.id.btnSaveHydration)
    }

    private fun setupSpinners() {
        // Interval spinner (10s for testing + 1-8 hours)
        val intervals = arrayOf("10s", "1h", "2h", "3h", "4h", "5h", "6h", "7h", "8h")
        val intervalAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, intervals)
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = intervalAdapter

        // Water goal spinner
        val waterGoals = arrayOf("4", "5", "6", "7", "8", "9", "10", "11", "12")
        val waterGoalAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, waterGoals)
        waterGoalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWaterGoal.adapter = waterGoalAdapter
    }

    private fun setupListeners() {
        switchHydration.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) enableControls() else {
                disableControls()
                cancelHydrationReminders()
            }
        }

        btnSaveHydration.setOnClickListener { saveSettings() }

        switchHabitReminders.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("habit_reminders_enabled", isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) "Habit reminders enabled" else "Habit reminders disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchMoodReminders.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("mood_reminders_enabled", isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) "Mood reminders enabled" else "Mood reminders disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadSettings() {
        val isHydrationEnabled = sharedPreferences.getBoolean("hydration_reminder_enabled", false)
        val intervalIndex = sharedPreferences.getInt("hydration_interval_index", 1)
        val waterGoal = sharedPreferences.getInt("water_goal", 8)

        switchHydration.isChecked = isHydrationEnabled
        spinnerInterval.setSelection(intervalIndex)
        spinnerWaterGoal.setSelection(waterGoal - 4)

        if (isHydrationEnabled) enableControls() else disableControls()

        switchHabitReminders.isChecked =
            sharedPreferences.getBoolean("habit_reminders_enabled", true)
        switchMoodReminders.isChecked =
            sharedPreferences.getBoolean("mood_reminders_enabled", true)
    }

    private fun saveSettings() {
        if (!switchHydration.isChecked) {
            Toast.makeText(requireContext(), "Enable hydration reminders first", Toast.LENGTH_SHORT).show()
            return
        }

        val intervalSelected = spinnerInterval.selectedItem.toString()
        val intervalMillis: Long = when (intervalSelected) {
            "10s" -> 10 * 1000L
            "1h" -> 1 * 60 * 60 * 1000L
            "2h" -> 2 * 60 * 60 * 1000L
            "3h" -> 3 * 60 * 60 * 1000L
            "4h" -> 4 * 60 * 60 * 1000L
            "5h" -> 5 * 60 * 60 * 1000L
            "6h" -> 6 * 60 * 60 * 1000L
            "7h" -> 7 * 60 * 60 * 1000L
            "8h" -> 8 * 60 * 60 * 1000L
            else -> 2 * 60 * 60 * 1000L
        }

        val waterGoal = spinnerWaterGoal.selectedItem.toString().toInt()
        sharedPreferences.edit()
            .putBoolean("hydration_reminder_enabled", true)
            .putInt("hydration_interval_index", spinnerInterval.selectedItemPosition)
            .putInt("water_goal", waterGoal)
            .apply()

        scheduleHydrationReminders(intervalMillis)

        Toast.makeText(requireContext(), "Settings saved! You'll receive reminders soon.", Toast.LENGTH_LONG).show()
    }

    private fun scheduleHydrationReminders(intervalMillis: Long) {
        cancelHydrationReminders()

        val intent = Intent(requireContext(), HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + intervalMillis

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMillis,
            pendingIntent
        )

        Log.d("SettingsFragment", "Hydration reminder scheduled every ${intervalMillis / 1000} seconds")
    }

    private fun cancelHydrationReminders() {
        val intent = Intent(requireContext(), HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun enableControls() {
        spinnerInterval.isEnabled = true
        spinnerWaterGoal.isEnabled = true
        btnSaveHydration.isEnabled = true
        btnSaveHydration.alpha = 1.0f
    }

    private fun disableControls() {
        spinnerInterval.isEnabled = false
        spinnerWaterGoal.isEnabled = false
        btnSaveHydration.isEnabled = false
        btnSaveHydration.alpha = 0.5f
    }
}
