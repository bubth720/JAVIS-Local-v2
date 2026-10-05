package com.stark.jarvislocal

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.Holder>() {
    private val items = mutableListOf<ChatMessage>()

    fun submit(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_message, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = items[position]
        val isUser = message.role == "user"
        val context = holder.itemView.context

        holder.row.gravity = if (isUser) Gravity.END else Gravity.START
        holder.author.text = if (isUser) "VOUS" else "JARVIS"
        holder.author.setTextColor(
            ContextCompat.getColor(
                context,
                if (isUser) R.color.jarvis_cyan else R.color.jarvis_green
            )
        )

        holder.card.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                if (isUser) R.color.jarvis_surface_2 else R.color.jarvis_panel
            )
        )
        holder.card.strokeColor = ContextCompat.getColor(context, R.color.jarvis_border)
        holder.text.text = message.text

        if (message.source.isNullOrBlank()) {
            holder.source.visibility = View.GONE
        } else {
            holder.source.visibility = View.VISIBLE
            holder.source.text = "Source : " + message.source
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val row: LinearLayout = view.findViewById(R.id.messageRow)
        val card: MaterialCardView = view.findViewById(R.id.messageCard)
        val author: TextView = view.findViewById(R.id.messageAuthor)
        val text: TextView = view.findViewById(R.id.messageText)
        val source: TextView = view.findViewById(R.id.messageSource)
    }
}
