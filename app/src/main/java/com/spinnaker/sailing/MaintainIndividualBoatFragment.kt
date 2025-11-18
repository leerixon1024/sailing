package com.spinnaker.sailing
import android.os.Bundle
import android.text.InputFilter
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.util.Log
import android.widget.Toast

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.spinnaker.sailing.ui.boat.BoatDao
import com.spinnaker.sailing.ui.boat.BoatEntity
import kotlinx.coroutines.launch


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER



/**
 * A simple [Fragment] subclass.
 * Use the [MaintainIndividualBoatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MaintainIndividualBoatFragment : Fragment(), View.OnClickListener{

    private lateinit var boatNameEditText: EditText
    private lateinit var boatSailNumberEditText: EditText
    private lateinit var boatLOAEditText: EditText
    private lateinit var boatPHRFEditText: EditText
    private lateinit var boatMakeEditText: EditText
    private lateinit var boatModelEditText: EditText
    private lateinit var addButton: Button
    private lateinit var boatDAO: BoatDao
    private var currentBoatId: Int = -1
    private var currentAction: String? = null
    lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_maintain_individual_boat, container, false)
    }
    override fun onCreate(savedInstanceState: Bundle?)

    {
        super.onCreate(savedInstanceState)

        // Access arguments here
        Log.d("MIBF_DEBUG", "Arguments object: $arguments")

        arguments?.let { bundle ->
            currentBoatId =
                bundle.getInt("boatId", -1) // -1 is a default value if "boatId" isn't found
            currentAction = bundle.getString("action")

        }
        Log.d("MIBF", "Came into edit Boat ID: $currentBoatId, Action: $currentAction")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_add_new).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonDeleteBoat).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonUpdateBoat).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonUpdateBoatPolar).setOnClickListener(this)
        boatNameEditText = view.findViewById(R.id.textViewBoatName)
        boatNameEditText.filters = arrayOf(InputFilter.AllCaps())
        boatSailNumberEditText = view.findViewById(R.id.textViewBoatSailNumber)
        boatLOAEditText = view.findViewById(R.id.textViewBoatLOA)
        boatModelEditText = view.findViewById(R.id.textViewBoatModelName)
        boatModelEditText.filters = arrayOf(InputFilter.AllCaps())
        boatMakeEditText = view.findViewById(R.id.textViewBoatMake)
        boatMakeEditText.filters = arrayOf(InputFilter.AllCaps())
        boatPHRFEditText = view.findViewById(R.id.textViewBoatPHRF)


        boatDAO = AppDatabase.getDatabase(requireContext()).BoatDao() // Initialize DAO

        Log.d("MIBF", "just before boat lookup Boat ID: $currentBoatId, Action: $currentAction")
        if (currentAction == "edit" && currentBoatId != -1)
        {
            Log.d("MIBF", "just before loadBoatData ID: $currentBoatId, Action: $currentAction")
            lifecycleScope.launch  { loadBoatDetails(currentBoatId) }
        }


    }

    override fun onClick(v: View?)
    {
        when(v!!.id)
        {
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.maintCourseFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.button_add_new -> {
                if (validateEntries())
                {
                    lifecycleScope.launch                     {
                    if (currentBoatId == -1 && currentAction == "new")
                        {
                             addNewBoatToDatabase()
                        }
                    else
                    if (currentAction == "edit" && currentBoatId != -1)
                        {
                            updateBoatInDatabase()
                        }

                    }
                }

            }




            R.id.buttonDeleteBoat -> {

                if (validateEntries()) {lifecycleScope.launch { deleteBoatInDatabase() }                }
            }




            R.id.buttonUpdateBoat -> {

                if (validateEntries())  {lifecycleScope.launch { updateBoatInDatabase() }            }
            }
            R.id.buttonUpdateBoatPolar -> {
                if (validateEntries())  {
                    val bundle = Bundle().apply {
                        putInt("boatId", currentBoatId)
                        putString("action", "list")}
                        Log.d("MIBF", "just before going to PerformaneDataFragment Boat ID: $currentBoatId")
                    navController.navigate(R.id.action_maintainIndividualBoatFragment_to_maintainPerformanceDataFragment, bundle)

                }
            }
        }
    }

    private fun validateEntries(): Boolean
    {
        if (boatNameEditText.text.toString().trim().isEmpty())
        {
            boatNameEditText.error = "Boat name cannot be empty"
            return false
        }
        if (boatSailNumberEditText.text.toString().trim().isEmpty())
        {
            boatSailNumberEditText.error = "Sail number cannot be empty"
            return false
        }
        if (boatLOAEditText.text.toString().trim().isEmpty())
        {
            boatLOAEditText.error = "LOA cannot be empty"
            return false
        }

        if (boatPHRFEditText.text.toString().trim().isEmpty())
        {
            boatPHRFEditText.error = "PHRF number cannot be empty"
            return false
        }
        if (boatMakeEditText.text.toString().trim().isEmpty())
        {
            boatMakeEditText.error = "Boat Make cannot be empty"
            return false
        }
        // Add more validations as needed for other fields
        return true
    }

    private suspend fun addNewBoatToDatabase()
        {
        val boatName = boatNameEditText.text.toString().trim()
        val boatSailNumber = boatSailNumberEditText.text.toString().trim().toIntOrNull() ?: 0
        val boatLOA = boatLOAEditText.text.toString().trim().toIntOrNull() ?: 0 // Default to 0 if not a valid number = textViewBoatSailNumber.text.toString().trim()
        val boatPHRF = boatPHRFEditText.text.toString().trim().toIntOrNull() ?: 0 // Default to 0 if not a valid number
        val boatMake = boatMakeEditText.text.toString().trim()
        val boatModel = boatModelEditText.text.toString().trim()
        // For simplicity, always adding a new boat here.

        val newBoat = BoatEntity(
            0,
            boatSailNumber,
            boatName,
            boatLOA,
            boatPHRF,
            boatMake,
            boatModel
        ) // ID 0 for new boat, DB will assign
        val result = boatDAO.addBoatById(newBoat)
        if (result != -1L)
            {
            Toast.makeText(context, "Boat added successfully", Toast.LENGTH_SHORT).show()
            navController.popBackStack() // Go back after adding
            }
        else
            {
            Toast.makeText(context, "Error adding boat", Toast.LENGTH_SHORT).show()
            }
        }
// update existing boat
    private suspend fun updateBoatInDatabase()
        {
        val boatName = boatNameEditText.text.toString().trim()
        val boatSailNumberInt = boatSailNumberEditText.text.toString().trim().toIntOrNull() ?: 0
        val boatLOAInt = boatLOAEditText.text.toString().trim().toIntOrNull() ?: 0 // Default to 0 if not a valid number = textViewBoatSailNumber.text.toString().trim()
        val boatPHRFInt = boatPHRFEditText.text.toString().trim().toIntOrNull() ?: 0 // Default to 0 if not a valid number
        val boatMake = boatMakeEditText.text.toString().trim()
        val boatModel = boatModelEditText.text.toString().trim()

            val boatToUpdate = BoatEntity( // Or 'Boat' if that's your class name
                id = currentBoatId,       // Should be Int
                boatName = boatName,          // Should be String
                boatSailNumber = boatSailNumberInt, // Should be Int
                lOA = boatLOAInt,         // Should be Int
                pHRF = boatPHRFInt,       // Should be Int
                boatMake = boatMake,          // Should be String
                boatModelName = boatModel         // Should be String
            )

            val rowsAffected = boatDAO.updateBoatById(boatToUpdate)

            if (rowsAffected > 0)
                { // Check if the update was successful
                Toast.makeText(context, "Boat updated successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack() // Go back after updating
                }
            else
                {
                Toast.makeText(context, "Error updating boat or no changes were made", Toast.LENGTH_SHORT).show()
                }
        }

    private suspend fun deleteBoatInDatabase() {
        if (currentBoatId != -1)
        {
            val boatToDelete = BoatEntity(
                id = currentBoatId, /* other fields can be default/dummy if not used by delete */
                boatSailNumber = 0, // Or retrieve the actual boat if needed
                boatName = "",
                lOA = 0,
                pHRF = 0,
                boatMake = "",
                boatModelName = ""
            )
            val rowsAffected = boatDAO.deleteBoatById(boatToDelete)
            if (rowsAffected > 0) {
                Toast.makeText(context, "Boat deleted successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack() // Go back after deleting
                }
            else
                {
                Toast.makeText(context, "Error deleting boat or boat not found", Toast.LENGTH_SHORT).show()
                }
        }
        else
        {
            Toast.makeText(context, "No boat selected for deletion", Toast.LENGTH_SHORT).show()
        }
    }





    private suspend fun loadBoatDetails(boatId: Int) {

        if (boatId != -1) {
            Toast.makeText(context, "Boat selected for loading", Toast.LENGTH_SHORT).show()

            val boat = boatDAO.getBoatById(boatId)
            boat?.let { boatEntity: BoatEntity ->
                Log.d("MIBF", "setting up boat details Boat ID: $currentBoatId, Action: $currentAction")
                boatNameEditText.setText(boatEntity.boatName)
                boatSailNumberEditText.setText(boatEntity.boatSailNumber.toString())
                boatLOAEditText.setText(boatEntity.lOA.toString())
                boatPHRFEditText.setText(boatEntity.pHRF.toString())
                boatMakeEditText.setText(boatEntity.boatMake)
                boatModelEditText.setText(boatEntity.boatModelName)
                Log.d("MIBF", "after setting up boat details Boat ID: $currentBoatId, Action: $currentAction")
            }

        }
        else
        {  Toast.makeText(context, "Boat not found for loading", Toast.LENGTH_SHORT).show() }

        }
}