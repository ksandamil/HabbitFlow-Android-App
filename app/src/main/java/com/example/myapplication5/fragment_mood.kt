package com.example.myapplication5

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MoodFragment : Fragment() {

    private lateinit var emojiContainer: LinearLayout
    private lateinit var etNote: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var rvHistory: RecyclerView
    private lateinit var lineChart: com.github.mikephil.charting.charts.LineChart
    private lateinit var calendarView: CalendarView
    private lateinit var btnShare7: MaterialButton
    private lateinit var tvMoodCount: TextView

    private val prefsKey = "mood_prefs"
    private val entriesKey = "mood_entries"

    private val gson = Gson()
    private var moodList = mutableListOf<MoodEntry>()
    private lateinit var adapter: MoodAdapter


    private val emojiOptions = listOf(
        Pair("😢", 1),
        Pair("😐", 2),
        Pair("🙂", 3),
        Pair("😊", 4),
        Pair("🤩", 5)
    )

    private var selectedEmoji: Pair<String, Int>? = null

    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mood, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emojiContainer = view.findViewById(R.id.emojiContainer)
        etNote = view.findViewById(R.id.etMoodNote)
        btnSave = view.findViewById(R.id.btnSaveMood)
        rvHistory = view.findViewById(R.id.rvMoodHistory)
        lineChart = view.findViewById(R.id.lineChart)
        calendarView = view.findViewById(R.id.calendarView)
        btnShare7 = view.findViewById(R.id.btnShare7)
        tvMoodCount = view.findViewById(R.id.tvMoodCount)
        val btnExport = view.findViewById<MaterialButton>(R.id.btnExport)
        btnExport.setOnClickListener { exportPrefs() }


        // RecyclerView setup
        adapter = MoodAdapter(requireContext(), moodList) { entry ->
            handleDeleteEntry(entry)
        }
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        // build emoji selector
        buildEmojiButtons()

        // load saved entries
        loadMoodEntries()

        // selectedDate default to today
        selectedDate = getDateFromMillis(calendarView.date)
        preselectForDate(selectedDate)

        // update total count initially
        updateMoodCount()

        // calendar date change
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val m = month + 1
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, m, dayOfMonth)
            preselectForDate(selectedDate)
        }

        // Save button
        btnSave.setOnClickListener { saveMoodForSelectedDate() }

        // Share 7-day
        btnShare7.setOnClickListener { shareLast7Days() }

        // chart
        setupChart()
        updateChart()
    }

    // build emoji buttons
    private fun buildEmojiButtons() {
        emojiContainer.removeAllViews()
        val pad = (8 * resources.displayMetrics.density).toInt()
        for ((emoji, value) in emojiOptions) {
            val b = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            )
            b.text = emoji
            b.textSize = 18f
            b.setPadding(pad, pad / 2, pad, pad / 2)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = pad / 2
            b.layoutParams = lp
            b.strokeWidth = 0
            b.setOnClickListener {
                for (i in 0 until emojiContainer.childCount) {
                    val ch = emojiContainer.getChildAt(i) as MaterialButton
                    ch.setBackgroundColor(Color.TRANSPARENT)
                    ch.setTextColor(Color.BLACK)
                }
                b.setBackgroundColor(Color.parseColor("#D1FAE5"))
                b.setTextColor(Color.parseColor("#065F46"))
                selectedEmoji = Pair(emoji, value)
            }
            emojiContainer.addView(b)
        }
    }

    private fun saveMoodForSelectedDate() {
        if (selectedEmoji == null) {
            Toast.makeText(requireContext(), "Please select an emoji for your mood", Toast.LENGTH_SHORT).show()
            return
        }
        val emoji = selectedEmoji!!.first
        val value = selectedEmoji!!.second
        val note = etNote.text?.toString()?.trim()
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = sdfTime.format(Date())

        val existingIndex = moodList.indexOfFirst { it.date == selectedDate }
        val newEntry = MoodEntry(
            date = selectedDate,
            time = time,
            emoji = emoji,
            note = if (note.isNullOrEmpty()) null else note,
            value = value
        )

        if (existingIndex >= 0) {
            moodList[existingIndex] = newEntry
        } else {
            moodList.add(0, newEntry)
            sortMoodList()
        }

        saveMoodEntries()
        adapter.updateList(moodList)
        updateChart()
        updateMoodCount()

        etNote.text?.clear()
        clearEmojiSelection()
        selectedEmoji = null

        Toast.makeText(requireContext(), "Mood saved for $selectedDate", Toast.LENGTH_SHORT).show()
    }

    private fun shareLast7Days() {
        val cal = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfPretty = SimpleDateFormat("EEE dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("My mood summary (last 7 days):\n")
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val key = sdfDate.format(cal.time)
            val pretty = sdfPretty.format(cal.time)
            val entry = moodList.firstOrNull { it.date == key }
            if (entry != null) {
                sb.append("$pretty: ${entry.emoji} ${entry.note?.let { " - $it" } ?: ""}\n")
            } else {
                sb.append("$pretty: —\n")
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
        startActivity(android.content.Intent.createChooser(shareIntent, "Share mood summary"))
    }

    private fun preselectForDate(date: String) {
        val entry = moodList.firstOrNull { it.date == date }
        if (entry != null) {
            etNote.setText(entry.note ?: "")
            for (i in 0 until emojiContainer.childCount) {
                val ch = emojiContainer.getChildAt(i) as MaterialButton
                if (ch.text.toString() == entry.emoji) {
                    ch.performClick()
                } else {
                    ch.setBackgroundColor(Color.TRANSPARENT)
                    ch.setTextColor(Color.BLACK)
                }
            }
        } else {
            etNote.setText("")
            clearEmojiSelection()
            selectedEmoji = null
        }
    }

    private fun clearEmojiSelection() {
        for (i in 0 until emojiContainer.childCount) {
            val ch = emojiContainer.getChildAt(i) as MaterialButton
            ch.setBackgroundColor(Color.TRANSPARENT)
            ch.setTextColor(Color.BLACK)
        }
    }

    private fun loadMoodEntries() {
        val prefs = requireContext().getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val json = prefs.getString(entriesKey, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            moodList = gson.fromJson(json, type)
            sortMoodList()
            adapter.updateList(moodList)
        }
        updateMoodCount()
    }

    private fun saveMoodEntries() {
        val prefs = requireContext().getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val json = gson.toJson(moodList)
        prefs.edit().putString(entriesKey, json).apply()
    }

    private fun sortMoodList() {
        moodList.sortWith(compareByDescending<MoodEntry> { it.date }.thenByDescending { it.time })
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.setScaleEnabled(false)
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setNoDataText("No mood data")

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.DKGRAY
        lineChart.axisLeft.granularity = 1f
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisLeft.axisMaximum = 6f
        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.animateY(600)
    }

    private fun updateChart() {
        val cal = Calendar.getInstance()
        val sdfLabel = SimpleDateFormat("EE", Locale.getDefault())
        val labels = ArrayList<String>()
        val values = ArrayList<Entry>()

        val moodByDate = mutableMapOf<String, Int>()
        for (m in moodList) {
            if (!moodByDate.containsKey(m.date)) moodByDate[m.date] = m.value
        }

        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val d = cal.time
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d)
            labels.add(sdfLabel.format(d))
            val v = moodByDate[key]?.toFloat() ?: 0f
            values.add(Entry(i.toFloat(), v))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val ds = LineDataSet(values, "Mood (1=low,5=high)")
        ds.mode = LineDataSet.Mode.CUBIC_BEZIER
        ds.setDrawValues(false)
        ds.setDrawCircles(true)
        ds.circleRadius = 5f
        ds.setCircleColor(Color.parseColor("#10B981"))
        ds.color = Color.parseColor("#10B981")
        ds.lineWidth = 2.2f
        ds.setDrawFilled(true)
        ds.fillColor = Color.parseColor("#D1FAE5")
        ds.fillAlpha = 180

        val data = LineData(ds)
        lineChart.data = data

        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelCount = labels.size
        lineChart.invalidate()
    }

    private fun handleDeleteEntry(entry: MoodEntry) {
        val index = moodList.indexOfFirst { it.date == entry.date && it.time == entry.time && it.emoji == entry.emoji }
        if (index < 0) return
        val removed = moodList.removeAt(index)
        saveMoodEntries()
        adapter.updateList(moodList)
        updateChart()
        updateMoodCount()

        Snackbar.make(requireView(), "Deleted mood on ${removed.date}", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                moodList.add(index, removed)
                sortMoodList()
                saveMoodEntries()
                adapter.updateList(moodList)
                updateChart()
                updateMoodCount()
            }.show()
    }

    private fun getDateFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun updateMoodCount() {
        tvMoodCount.text = "${moodList.size} entries"
    }


    private fun exportPrefs() {
        val prefs = requireContext().getSharedPreferences(prefsKey, Context.MODE_PRIVATE)
        val json = prefs.getString(entriesKey, null)
        val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloads, "mood_backup.txt")
        file.writeText(json ?: "No data found")
        Toast.makeText(requireContext(), "Exported to Downloads folder!", Toast.LENGTH_LONG).show()
    }


}
