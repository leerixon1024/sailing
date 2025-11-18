package com.spinnaker.sailing.ui.mark  // Or your preferred package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spinnaker.sailing.R

class MarkListAdapter(
    private val onItemClick: (MarkEntity) -> Unit // Lambda for click events
) : ListAdapter<MarkEntity, MarkListAdapter.MarkViewHolder>(MarkDiffCallback()) {

    // ViewHolder describes an item view and metadata about its place within the RecyclerView.
    class MarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val markNameTextView: TextView = itemView.findViewById(R.id.textViewMarkName)

        fun bind(mark: MarkEntity, onItemClick: (MarkEntity) -> Unit) {
            markNameTextView.text = mark.markName

            itemView.setOnClickListener { onItemClick(mark) }
        }
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marklist, parent, false) // Inflate your item layout
        return MarkViewHolder(view)
    }

    // Called by RecyclerView to display the data at the specified position.
    override fun onBindViewHolder(holder: MarkViewHolder, position: Int) {
        val currentMark = getItem(position) // getItem() is provided by ListAdapter
        holder.bind(currentMark, onItemClick)
    }

    // DiffUtil.ItemCallback implementation to calculate list differences
    class MarkDiffCallback : DiffUtil.ItemCallback<MarkEntity>() {
        override fun areItemsTheSame(oldItem: MarkEntity, newItem: MarkEntity): Boolean {
            // Check if items represent the same object (e.g., by unique ID)
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MarkEntity, newItem: MarkEntity): Boolean {
            // Check if the content of the items is the same
            // This is called only if areItemsTheSame() returns true
            return oldItem == newItem // Relies on Boat being a data class for proper equals()
        }
    }
}