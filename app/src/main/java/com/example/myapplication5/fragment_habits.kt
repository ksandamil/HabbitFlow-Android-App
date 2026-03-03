package com.example.myapplication5

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class HabitsFragment : Fragment() {

    private lateinit var habitContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvHabitCount: TextView
    private lateinit var btnAddHabit: Button
    private lateinit var etNewHabit: EditText

    private val prefsName = "habits_prefs"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_habits, container, false)

        habitContainer = view.findViewById(R.id.habitListContainer)
        progressBar = view.findViewById(R.id.progressBar)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
        tvHabitCount = view.findViewById(R.id.tvHabitCount)
        btnAddHabit = view.findViewById(R.id.btnAddHabit)
        etNewHabit = view.findViewById(R.id.etNewHabit)

        loadHabits()
        updateProgress()
        updateHabitCount()

        btnAddHabit.setOnClickListener {
            val habitName = etNewHabit.text.toString().trim()
            if (habitName.isNotEmpty()) {
                addHabit(habitName)
                etNewHabit.text.clear()
            } else {
                Toast.makeText(requireContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadHabits() {
        habitContainer.removeAllViews()
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val habits = prefs.getStringSet("habits", mutableSetOf()) ?: mutableSetOf()
        for (habit in habits) {
            addHabitView(habit)
        }
        updateHabitCount()
    }

    private fun addHabitView(habit: String) {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_habit, habitContainer, false)

        val cbHabit = itemView.findViewById<CheckBox>(R.id.cbHabit)
        val btnEdit = itemView.findViewById<ImageButton>(R.id.btnEdit)
        val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDelete)

        cbHabit.text = habit
        cbHabit.isChecked = isHabitCompletedToday(habit)

        cbHabit.setOnClickListener {
            val checked = cbHabit.isChecked
            setHabitCompletedToday(habit, checked)
            updateProgress()
        }

        btnEdit.setOnClickListener {
            showEditHabitDialog(habit)
        }

        btnDelete.setOnClickListener {
            deleteHabit(habit)
        }

        habitContainer.addView(itemView)
    }

    private fun showEditHabitDialog(oldHabitName: String) {
        val editText = EditText(requireContext())
        editText.hint = "Edit habit name"
        editText.setText(oldHabitName)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Habit")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    editHabit(oldHabitName, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editHabit(oldHabitName: String, newHabitName: String) {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val habits = prefs.getStringSet("habits", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (habits.contains(oldHabitName)) {
            habits.remove(oldHabitName)
            habits.add(newHabitName)
            prefs.edit().putStringSet("habits", habits).apply()

            val completed = isHabitCompletedToday(oldHabitName)
            setHabitCompletedToday(oldHabitName, false)
            setHabitCompletedToday(newHabitName, completed)

            loadHabits()
            updateProgress()
            updateHabitCount()
        }
    }

    private fun deleteHabit(habit: String) {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val habits = prefs.getStringSet("habits", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (habits.remove(habit)) {
            prefs.edit().putStringSet("habits", habits).apply()
            setHabitCompletedToday(habit, false)
            loadHabits()
            updateProgress()
            updateHabitCount()
        }
    }

    private fun addHabit(habit: String) {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val habits = prefs.getStringSet("habits", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        habits.add(habit)
        prefs.edit().putStringSet("habits", habits).apply()
        addHabitView(habit)
        updateProgress()
        updateHabitCount()
    }

    private fun isHabitCompletedToday(habit: String): Boolean {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val todayKey = "completed_${habit}_${todayDateKey()}"
        return prefs.getBoolean(todayKey, false)
    }

    private fun setHabitCompletedToday(habit: String, completed: Boolean) {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val todayKey = "completed_${habit}_${todayDateKey()}"
        prefs.edit().putBoolean(todayKey, completed).apply()
    }

    private fun todayDateKey(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    private fun updateProgress() {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val habits = prefs.getStringSet("habits", mutableSetOf()) ?: mutableSetOf()
        if (habits.isEmpty()) {
            progressBar.progress = 0
            tvProgressPercent.text = "0% Completed"
            return
        }
        var completedCount = 0
        for (habit in habits) {
            if (isHabitCompletedToday(habit)) completedCount++
        }
        val percent = (completedCount * 100) / habits.size
        progressBar.progress = percent
        tvProgressPercent.text = "$percent% Completed"
    }

    private fun updateHabitCount() {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val habits = prefs.getStringSet("habits", mutableSetOf()) ?: mutableSetOf()
        tvHabitCount.text = "${habits.size} habits"
    }
}
