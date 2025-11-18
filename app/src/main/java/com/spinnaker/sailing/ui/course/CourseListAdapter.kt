package com.spinnaker.sailing.ui.course


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spinnaker.sailing.R

class CourseListAdapter(
    private val onItemClick: (CourseEntity) -> Unit // Lambda for click events
) : ListAdapter<CourseEntity, CourseListAdapter.CourseViewHolder>(CourseDiffCallback()) {

    // ViewHolder describes an item view and metadata about its place within the RecyclerView.
    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val courseDescTextView: TextView = itemView.findViewById(R.id.textViewCourseDesc)

        fun bind(course: CourseEntity, onItemClick: (CourseEntity) -> Unit) {
            courseDescTextView.text = course.courseDesc

            itemView.setOnClickListener { onItemClick(course) }
        }
    }

    // Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_courselist, parent, false) // Inflate your item layout
        return CourseViewHolder(view)
    }

    // Called by RecyclerView to display the data at the specified position.
    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val currentCourse = getItem(position) // getItem() is provided by ListAdapter
        holder.bind(currentCourse, onItemClick)
    }

    // DiffUtil.ItemCallback implementation to calculate list differences
    class CourseDiffCallback : DiffUtil.ItemCallback<CourseEntity>() {
        override fun areItemsTheSame(oldItem: CourseEntity, newItem: CourseEntity): Boolean {
            // Check if items represent the same object (e.g., by unique ID)
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CourseEntity, newItem: CourseEntity): Boolean {
            // Check if the content of the items is the same
            // This is called only if areItemsTheSame() returns true
            return oldItem == newItem // Relies on Boat being a data class for proper equals()
        }
    }
}