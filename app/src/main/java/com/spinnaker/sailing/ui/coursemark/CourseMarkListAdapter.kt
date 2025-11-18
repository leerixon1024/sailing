package com.spinnaker.sailing.ui.coursemark


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spinnaker.sailing.R

class CourseMarkListAdapter(
    private val onItemClick: (CourseMarkWithMark) -> Unit // Lambda for click events
) : ListAdapter<CourseMarkWithMark, CourseMarkListAdapter.CourseMarkViewHolder>(CourseMarkDiffCallback()) {

    // ViewHolder describes an item view and metadata about its place within the RecyclerView.
    class CourseMarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val courseMarkDescTextView: TextView = itemView.findViewById(R.id.textViewCourseMarkName)

        fun bind(courseMark: CourseMarkWithMark, onItemClick: (CourseMarkWithMark) -> Unit) {
            courseMarkDescTextView.text = courseMark.markName

            itemView.setOnClickListener { onItemClick(courseMark) }
        }
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseMarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coursemarklist, parent, false) // Inflate your item layout for course marks
        return CourseMarkViewHolder(view)
    }

    // Called by RecyclerView to display the data at the specified position.
    override fun onBindViewHolder(holder: CourseMarkViewHolder, position: Int) {
        val currentCourseMark = getItem(position) // getItem() is provided by ListAdapter
        holder.bind(currentCourseMark, onItemClick)
    }

    // DiffUtil.ItemCallback implementation to calculate list differences
    class CourseMarkDiffCallback : DiffUtil.ItemCallback<CourseMarkWithMark>() {
        override fun areItemsTheSame(oldItem: CourseMarkWithMark, newItem: CourseMarkWithMark): Boolean {
            // Check if items represent the same object (e.g., by unique ID)
            return oldItem.courseMark.id == newItem.courseMark.id
        }

        override fun areContentsTheSame(oldItem: CourseMarkWithMark, newItem: CourseMarkWithMark): Boolean {
            // Check if the content of the items is the same
            // This is called only if areItemsTheSame() returns true
            return oldItem == newItem
        }
    }
}