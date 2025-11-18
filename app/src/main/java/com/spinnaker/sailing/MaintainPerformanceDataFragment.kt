package com.spinnaker.sailing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import androidx.lifecycle.lifecycleScope

import androidx.navigation.NavController
import androidx.navigation.Navigation

import androidx.recyclerview.widget.LinearLayoutManager

import com.spinnaker.sailing.databinding.FragmentMaintPerformancedataBinding
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceViewModel
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceViewModelFactory
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlin.getValue

class MaintainPerformanceDataFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController

    private var _binding: FragmentMaintPerformancedataBinding? = null
    private val binding get() = _binding!!
    private var currentBoatId: Int = -1
    private val boatPerformanceDao by lazy {
        AppDatabase.getDatabase(requireContext()).BoatPerformanceDao()
    }
    private val boatPerformanceViewModel: BoatPerformanceViewModel by viewModels {
        BoatPerformanceViewModelFactory(boatPerformanceDao)
    }
    private val boatDao by lazy {
        AppDatabase.getDatabase(requireContext()).BoatDao()
    }

    private lateinit var boatPerformanceListAdapter: BoatPerformanceListAdapter // Your RecyclerView adapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaintPerformancedataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("MPDF", "Entered onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_add_new).setOnClickListener(this)

        setupRecyclerView()




        arguments?.let { bundle ->
            currentBoatId = bundle.getInt("boatId", -1)
        }
        Log.d("MPDF", "just after recycler view setup currentBoat ID $currentBoatId")
        if (currentBoatId != -1) {
            // Fetch boat name and set it
            viewLifecycleOwner.lifecycleScope.launch {
                val boat = withContext(Dispatchers.IO) {
                    boatDao.getBoatById(currentBoatId)
                }
                boat?.let {
                    binding.textViewBoatName.text = it.boatName
                }
            }

            // Check if valid
            Log.d("MPDF", "Boat ID found $currentBoatId")
            observeBoatPerformanceList(currentBoatId)
        } else {
            Log.d("MPFD", "Boat ID not found or invalid.")
            // Handle case where boatId is not available
            boatPerformanceListAdapter.submitList(emptyList()) // Show empty list
        }


    }

    override fun onClick(v: View?) {
        val bundle = Bundle()
        when (v!!.id) {
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.action_maintBoatFragment_to_maintainIndividualBoatFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.button_add_new -> {
                bundle.putString("action", "new")

                // performance id is passed as we are adding a new performance
                bundle.putInt("performanceId", -1)
                bundle.putInt("boatId", currentBoatId)
                navController.navigate(
                    R.id.action_maintainPerformanceDataFragment_to_maintainIndividualPerformanceFragment,
                    bundle
                )
            }

        }
    }

    private fun setupRecyclerView() {
        val bundle = Bundle()
        Log.d("MPDF", "Going through setup recycler view")
        boatPerformanceListAdapter = BoatPerformanceListAdapter(
            onItemClick = { clickedPerformanceEntity ->
                bundle.putInt("performanceId", clickedPerformanceEntity.id)
                bundle.putString("action", "edit")
                bundle.putInt("boatId", clickedPerformanceEntity.boatId)
                navController.navigate(
                    R.id.action_maintainPerformanceDataFragment_to_maintainIndividualPerformanceFragment,
                    bundle
                )
                // Handle click on a performance: Navigate to an edit screen or show a dialog
                // You'll likely pass boat.id to the next screen/dialog

            }

        )
        binding.recyclerViewPerformance.apply {
            Log.d("MPDF", "Going through recycler view performance")
            layoutManager = LinearLayoutManager(context)
            adapter = boatPerformanceListAdapter // Set the Adapter
        }
    }

    private fun observeBoatPerformanceList(boatId: Int) { // Pass boatId if needed
        Log.d("MPDF ", "Entered Observe PerformanceList")
        // Use the lifecycleScope of the viewLifecycleOwner.
        // This ensures the coroutine is cancelled when the Fragment's view is destroyed.
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Call the suspend function from a background thread
                val performanceList = withContext(Dispatchers.IO) {
                    Log.d("MPDF", "OBP Entered Setting up performance list $boatId")
                    boatPerformanceDao.getSpecificBoatPerformance(boatId)

                }
                // Update the UI on the main thread
                Log.d("MPDF", "OBP lets see the performance list $performanceList")
                boatPerformanceListAdapter.submitList(performanceList)
                Log.d("MPDF", "OBP Fetched list. Size: ${performanceList.size}")

            } catch (e: Exception) {
                Log.e("MPDF", "OBP Error fetching performance data", e)
                // Handle error, e.g., show a message to the user
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}