package com.example.myapplication5

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MoodAdapter(
    private val ctx: Context,
    private var items: List<MoodEntry>,
    private val onDelete: (MoodEntry) -> Unit
) : RecyclerView.Adapter<MoodAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)
        val tvNote: TextView = view.findViewById(R.id.tvMoodNote)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnShare: ImageButton = view.findViewById(R.id.btnShareMood)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteMood)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.tvEmoji.text = m.emoji
        holder.tvNote.text = m.note ?: ""
        holder.tvTime.text = m.time
        holder.tvDate.text = m.date

        holder.btnShare.setOnClickListener {
            val shareText = "My mood on ${m.date} ${m.time}: ${m.emoji}" +
                    (if (!m.note.isNullOrBlank()) "\nNote: ${m.note}" else "")
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, shareText)
            ctx.startActivity(Intent.createChooser(intent, "Share mood"))
        }

        holder.btnDelete.setOnClickListener {
            onDelete(m)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<MoodEntry>) {
        items = newList
        notifyDataSetChanged()
    }
}
