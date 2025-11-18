package com.spinnaker.sailing

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.util.Log
import android.widget.Toast
import android.widget.TextView

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import com.spinnaker.sailing.ui.boat.BoatDao
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class MaintainIndividualPerformanceFragment : Fragment(), View.OnClickListener {

    private lateinit var trueWindEditText: EditText
    private lateinit var tackAngleNumberEditText: EditText
    private lateinit var gybeAngleNumberEditText: EditText
    private lateinit var targetUpwindSpeedEditText: EditText
    private lateinit var targetOffwindSpeedEditText: EditText
    private lateinit var turnaroundtimeEditText: EditText
    private lateinit var leewayEditText: EditText


    private lateinit var boatPerformanceDAO: BoatPerformanceDao

    private lateinit var boatDAO: BoatDao
    private var currentPerformanceId: Int = -1
    private var currentBoatId: Int = -1
    private var currentBoatName: String? = null
    private var currentAction: String? = null
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MIBPF_DEBUG", "Arguments object: $arguments")
        arguments?.let { bundle ->
            currentBoatId =
                bundle.getInt("boatId", -1) // -1 is a default value if "boatId" isn't found
            currentAction = bundle.getString("action")
            currentPerformanceId = bundle.getInt("performanceId", -1)

        }
        // Access arguments here

        Log.d(
            "MIBPF",
            "Came into edit Perf ID: $currentPerformanceId, Action: $currentAction, Boat Id: $currentBoatId"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_maintain_individual_performance, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonAddNewPerformance).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonDeletePerformance).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonUpdatePerformance).setOnClickListener(this)

        trueWindEditText = view.findViewById(R.id.textViewTrueWind)
        tackAngleNumberEditText = view.findViewById(R.id.textViewTackAngle)
        gybeAngleNumberEditText = view.findViewById(R.id.textViewGybeAngle)
        targetUpwindSpeedEditText = view.findViewById(R.id.textViewTargetUpwindSpeed)
        targetOffwindSpeedEditText = view.findViewById(R.id.textViewTargetOffwindSpeed)
        turnaroundtimeEditText = view.findViewById(R.id.textViewTurnaroundtime)
        leewayEditText = view.findViewById(R.id.textViewLeeway)


        boatPerformanceDAO =
            AppDatabase.getDatabase(requireContext()).BoatPerformanceDao() // Initialize DAO

        boatDAO =
            AppDatabase.getDatabase(requireContext()).BoatDao() // Initialize DAO

        // Fetch boat details and set the boat name
        lifecycleScope.launch {
            val boat = boatDAO.getBoatById(currentBoatId)
            boat?.let {
                withContext(Dispatchers.Main) {
                    currentBoatName = it.boatName
                    view.findViewById<TextView>(R.id.textViewBoatName).text = it.boatName
                }
            }
        }

        Log.d(
            "MIPF",
            "just before performance lookup performance ID: $currentPerformanceId, Action: $currentAction"
        )
        if (currentAction == "edit" && currentPerformanceId != -1) {
            Log.d(
                "MIPF",
                "just before load BoatPerformanceData ID: $currentPerformanceId, Action: $currentAction"
            )
            lifecycleScope.launch { loadPerformanceDetails(currentPerformanceId) }
        }


    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.maintCourseFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.buttonAddNewPerformance -> {
                if (validateEntries()) {
                    lifecycleScope.launch {
                        if (currentPerformanceId == -1 && currentAction == "new") {
                            addNewPerformance()
                        } else
                            if (currentAction == "edit" && currentPerformanceId != -1) {
                                updatePerformance()
                            }

                    }
                }

            }


            R.id.buttonDeletePerformance -> {

                if (validateEntries()) {
                    lifecycleScope.launch { deletePerformance() }

                    Log.d(
                        "MIPF",
                        "just after update performance button clicked with valid entries$currentBoatId"
                    )

                    val bundle = Bundle().apply {
                        Log.d(
                            "MIPF",
                            "just after update performance button clicked with valid entries $currentBoatId"
                        )
                        putString("action", "edit")
                        putInt("boatId", currentBoatId)
                        putInt("performanceId", currentPerformanceId)
                    }

                    navController.navigate(
                        R.id.action_maintainIndividualPerformanceFragment_to_maintainPerformanceDataFragment,
                        bundle
                    )
                }
            }


            R.id.buttonUpdatePerformance -> {

                Log.d(
                    "MIPF",
                    "just after update performance button clicked prior to checking validity $currentBoatId"
                )
                if (validateEntries()) {
                    lifecycleScope.launch { updatePerformance() }
                    Log.d(
                        "MIPF",
                        "just after update performance button clicked with valid entries$currentBoatId"
                    )

                    val bundle = Bundle().apply {
                        Log.d(
                            "MIPF",
                            "just after update performance button clicked with valid entries $currentBoatId"
                        )
                        putString("action", "edit")
                        putInt("boatId", currentBoatId)
                        putInt("performanceId", currentPerformanceId)
                    }

                    navController.navigate(
                        R.id.action_maintainIndividualPerformanceFragment_to_maintainPerformanceDataFragment,
                        bundle
                    )

                }
            }
        }
    }

    private fun validateEntries(): Boolean {
        if (trueWindEditText.text.toString().trim().isEmpty()) {
            trueWindEditText.error = "True Wind cannot be empty"
            return false
        }
        if (tackAngleNumberEditText.text.toString().trim().isEmpty()) {
            tackAngleNumberEditText.error = "Tack Angle  cannot be empty"
            return false
        }

        if (gybeAngleNumberEditText.text.toString().trim().isEmpty()) {
            gybeAngleNumberEditText.error = "Gybe Angle  cannot be empty"
            return false
        }

        if (targetUpwindSpeedEditText.text.toString().trim().isEmpty()) {
            targetUpwindSpeedEditText.error = "Upwind Target Speed cannot be empty"
            return false
        }

        if (targetOffwindSpeedEditText.text.toString().trim().isEmpty()) {
            targetOffwindSpeedEditText.error = "Offwind Target Speed cannot be empty"
            return false
        }

        if (turnaroundtimeEditText.text.toString().trim().isEmpty()) {
            turnaroundtimeEditText.error = "turnaround time  cannot be empty"
            return false
        }

        if (leewayEditText.text.toString().trim().isEmpty()) {
            leewayEditText.error = "Leeway cannot be empty"
            return false
        }

        // Add more validations as needed for other fields
        return true
    }

    private suspend fun addNewPerformance() {
        val trueWind = trueWindEditText.text.toString().trim().toIntOrNull() ?: 0
        val tackAngle = tackAngleNumberEditText.text.toString().trim().toIntOrNull() ?: 0
        val gybeAngle = gybeAngleNumberEditText.text.toString().trim().toIntOrNull() ?: 0
        val targetSpeedUpwind = targetUpwindSpeedEditText.text.toString().trim().toFloatOrNull()
            ?: 0f // Default to 0 if not a valid number = textViewBoatSailNumber.text.toString().trim()
        val targetSpeedOffwind = targetOffwindSpeedEditText.text.toString().trim().toFloatOrNull()
            ?: 0f // Default to 0 if not a valid number = textViewBoatSailNumber.text.toString().trim()
        val turnaroundTimeInSeconds =
            turnaroundtimeEditText.text.toString().trim().toIntOrNull() ?: 0
        val leeway = leewayEditText.text.toString().trim().toDoubleOrNull() ?: 0.0


        val newPerformance = BoatPerformanceEntity(
            0, currentBoatId,
            trueWind, tackAngle, gybeAngle, targetSpeedUpwind,targetSpeedOffwind,turnaroundTimeInSeconds, leeway
        ) // ID 0 for new boat, DB will assign
        Log.d("MIBPF", "adding new performance boat Id: $currentBoatId")
        val result = boatPerformanceDAO.insertBoatPerformance(newPerformance)
        if (result != -1L) {
            Toast.makeText(context, "Performance added successfully", Toast.LENGTH_SHORT).show()
            val bundle = Bundle().apply {
                putInt("Id", id)
                putString("action", "edit")
                putInt("boatId", currentBoatId)
            }



            navController.navigate(
                R.id.action_maintainIndividualPerformanceFragment_to_maintainPerformanceDataFragment,
                bundle
            )
        } else {
            Toast.makeText(context, "Error adding performance", Toast.LENGTH_SHORT).show()
        }
    }

    // update existing performance
    private suspend fun updatePerformance() {
        val trueWindInt = trueWindEditText.text.toString().trim().toIntOrNull() ?: 0
        val tackAngleInt = tackAngleNumberEditText.text.toString().trim().toIntOrNull() ?: 0
        val gybeAngleInt = gybeAngleNumberEditText.text.toString().trim().toIntOrNull() ?: 0
        val targetSpeedUpwindFloat = targetUpwindSpeedEditText.text.toString().trim().toFloatOrNull()
            ?: 0f // Default to 0 if not a valid number
        val targetSpeedOffwindFloat = targetOffwindSpeedEditText.text.toString().trim().toFloatOrNull()
            ?: 0f // Default to 0 if not a valid number
        val turnaroundTimeInSecondsInt = turnaroundtimeEditText.text.toString().trim().toIntOrNull()
            ?: 0 // Default to 0 if not a valid number
        val leewayDouble = leewayEditText.text.toString().trim().toDoubleOrNull() ?: 0.0


        val performanceToUpdate = BoatPerformanceEntity( // Or 'Boat' if that's your class name
            id = currentPerformanceId,       // Should be Int
            boatId = currentBoatId,
            trueWind = trueWindInt,   // Should be Int
            tackAngle = tackAngleInt, // Should be Int
            gybeAngle = gybeAngleInt, // Should be Int
            targetSpeedUpwind = targetSpeedUpwindFloat, // Should be Float
            targetSpeedOffwind = targetSpeedUpwindFloat, // Should be Float
            turnaroundTimeInSeconds = turnaroundTimeInSecondsInt, // Should be Int
            leeway = leewayDouble

        )

        val rowsAffected = boatPerformanceDAO.updateBoatPerformance(performanceToUpdate)
        Log.d(
            "MIBPF",
            "just after update of Performance currentBoat ID $currentBoatId, rows affected $rowsAffected"
        )
        if (rowsAffected > 0) { // Check if the update was successful
            Toast.makeText(context, "Performance updated successfully", Toast.LENGTH_SHORT).show()
            Log.d(
                "MIBPF",
                "just after update of Performance currentBoat ID $currentBoatId, rows affected $rowsAffected"
            )


        } else {
            Toast.makeText(
                context,
                "Error updating performance or no changes were made",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun deletePerformance() {
        if (currentPerformanceId != -1) {
            val performanceToDelete = BoatPerformanceEntity(
                id = currentPerformanceId, /* other fields can be default/dummy if not used by delete */
                trueWind = 0, // Or retrieve the actual boat if needed
                tackAngle = 0,
                gybeAngle = 0,
                targetSpeedUpwind = 0F,
                targetSpeedOffwind = 0F,
                turnaroundTimeInSeconds = 0,
                leeway = 0.0
                )
            val rowsAffected = boatPerformanceDAO.deleteBoatPerformance(performanceToDelete)
            Log.d(
                "MIBPF",
                "just after delete of Performance currentBoat ID $currentBoatId, rows affected $rowsAffected"
            )

            if (rowsAffected > 0) {
                Toast.makeText(context, "Performance deleted successfully", Toast.LENGTH_SHORT)
                    .show()

            } else {
                Toast.makeText(
                    context,
                    "Error deleting performance or performance not found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "No performance selected for deletion", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private suspend fun loadPerformanceDetails(performanceId: Int) {

        if (performanceId != -1) {
            Toast.makeText(context, "Performance selected for loading", Toast.LENGTH_SHORT).show()

            val performance = boatPerformanceDAO.getOneSpecificBoatPerformance(performanceId)
            performance?.let { boatPerformanceEntity: BoatPerformanceEntity ->

                trueWindEditText.setText(boatPerformanceEntity.trueWind.toString())
                tackAngleNumberEditText.setText(boatPerformanceEntity.tackAngle.toString())
                gybeAngleNumberEditText.setText(boatPerformanceEntity.gybeAngle.toString())
                targetUpwindSpeedEditText.setText(boatPerformanceEntity.targetSpeedUpwind.toString())
                targetOffwindSpeedEditText.setText(boatPerformanceEntity.targetSpeedOffwind.toString())
                turnaroundtimeEditText.setText(boatPerformanceEntity.turnaroundTimeInSeconds.toString())
                leewayEditText.setText(boatPerformanceEntity.leeway.toString())

                Log.d(
                    "MIPF",
                    "after setting up boat details Boat ID: $currentBoatId, Action: $currentAction"
                )
            }

        } else {
            Toast.makeText(context, "Performance not found for loading", Toast.LENGTH_SHORT).show()
        }

    }
}