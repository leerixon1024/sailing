package com.spinnaker.sailing.ui.boatperformance

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spinnaker.sailing.R // Make sure this R is your project's R

// 1. Define the ViewHolder class clearly.
//    It should be an inner class or a standalone class that RecyclerView.Adapter can access.
class BoatPerformanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val trueWindTextView: TextView = view.findViewById(R.id.textViewTrueWind)
    private val tackAngleNumberTextView: TextView = view.findViewById(R.id.textViewTackAngle)



    init {


    }

    fun bind(performanceEntity: BoatPerformanceEntity, clickListener: (BoatPerformanceEntity) -> Unit) {
        Log.d("BPLA ViewHolderDebug", "Binding data for ID: ${performanceEntity.id}")
        Log.d("BPLAViewHolderDebug", "Raw trueWind: ${performanceEntity.trueWind}, boatId: ${performanceEntity.boatId}")

        trueWindTextView.text = performanceEntity.trueWind.toString()
        tackAngleNumberTextView.text = performanceEntity.tackAngle.toString()



        itemView.setOnClickListener {
            clickListener(performanceEntity)
        }
    }
}


// 2. Specify the ViewHolder type for ListAdapter.
class BoatPerformanceListAdapter(
    private val onItemClick: (clickedEntity: BoatPerformanceEntity) -> Unit
) : ListAdapter<BoatPerformanceEntity, BoatPerformanceViewHolder>(BoatDiffCallback()) { // Use BoatPerformanceViewHolder here

    // 3. onCreateViewHolder must return an instance of the specified ViewHolder type (BoatPerformanceViewHolder).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoatPerformanceViewHolder {
        Log.d("AdapterDebug", "onCreateViewHolder called, viewType: $viewType") // Add this

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_boatperformance, parent, false) // Ensure this is your correct item layout file
        return BoatPerformanceViewHolder(view)
    }

    // 4. onBindViewHolder's 'holder' parameter must be of the specified ViewHolder type (BoatPerformanceViewHolder).
    override fun onBindViewHolder(holder: BoatPerformanceViewHolder, position: Int) {


        val currentBoatPerformance = getItem(position)
        Log.d("BPLA AdapterDebug", "onBindViewHolder called, position: $position") // Add this

        holder.bind(currentBoatPerformance, onItemClick)
    }
}

// DiffUtil.ItemCallback implementation
class BoatDiffCallback : DiffUtil.ItemCallback<BoatPerformanceEntity>() {
    override fun areItemsTheSame(oldItem: BoatPerformanceEntity, newItem: BoatPerformanceEntity): Boolean {
        // Ensure BoatPerformanceEntity has a stable unique ID, e.g., 'id' or 'performanceId'
        return oldItem.id == newItem.id // Replace 'id' with your actual unique identifier property
    }

    override fun areContentsTheSame(oldItem: BoatPerformanceEntity, newItem: BoatPerformanceEntity): Boolean {
        // This is called only if areItemsTheSame() returns true.
        // If BoatPerformanceEntity is a data class, '==' will compare content.
        return oldItem == newItem
    }
}
