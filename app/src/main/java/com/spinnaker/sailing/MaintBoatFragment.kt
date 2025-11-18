package com.spinnaker.sailing

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spinnaker.sailing.databinding.FragmentMaintBoatBinding // ViewBinding
import com.spinnaker.sailing.ui.boat.BoatEntity
import com.spinnaker.sailing.ui.boat.BoatListAdapter
import com.spinnaker.sailing.ui.boat.BoatViewModel
import com.spinnaker.sailing.ui.boat.BoatViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader


class MaintBoatFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController

    // Assuming you are using ViewBinding
    private var _binding: FragmentMaintBoatBinding? = null
    private val binding get() = _binding!!

    // Get the BoatDao instance from your Room Database
    // This is a simplified example; you'd typically use Hilt or manual DI
    private val boatDao by lazy {
        AppDatabase.getDatabase(requireContext()).BoatDao()
    }

    private val boatViewModel: BoatViewModel by viewModels {
        BoatViewModelFactory(boatDao)
    }

    private lateinit var boatAdapter: BoatListAdapter // Your RecyclerView adapter

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                writeBoatsToUri(uri)
            }
        }
    }

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                readBoatsFromUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMaintBoatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        binding.leftBtn.setOnClickListener(this)
        binding.rightBtn.setOnClickListener(this)
        binding.homeBtn.setOnClickListener(this)
        binding.buttonAddNew.setOnClickListener(this)
        binding.buttonBackupAll.setOnClickListener(this)
        binding.buttonRestoreAll.setOnClickListener(this)
        // load all boats from database
        setupRecyclerView()
        observeBoatList()

    }

    override fun onClick(v: View?) {
        val bundle = Bundle()
        when (v!!.id) {
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.action_maintBoatFragment_to_maintainIndividualBoatFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.button_add_new -> {
                bundle.putString("action", "new")
                bundle.putInt("boatId", -1)
                navController.navigate(
                    R.id.action_maintBoatFragment_to_maintainIndividualBoatFragment,
                    bundle
                )
            }
            R.id.button_backup_all -> {
                saveFileLauncher.launch("boats_backup.json")
            }
            R.id.button_restore_all -> {
                openFileLauncher.launch(arrayOf("application/json"))
            }
        }
    }

    private suspend fun writeBoatsToUri(uri: Uri) {
        val boats = boatDao.getAllBoats().first()
        val gson = Gson()
        val json = gson.toJson(boats)

        try {
            withContext(Dispatchers.IO) {
                requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(json.toByteArray())
                    }
                }
            }
            Toast.makeText(context, "Boats backed up successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to backup boats: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun readBoatsFromUri(uri: Uri) {
        val gson = Gson()
        val boatListType = object : TypeToken<List<BoatEntity>>() {}.type
        try {
            val json = withContext(Dispatchers.IO) {
                requireContext().contentResolver.openInputStream(uri)?.use {
                    BufferedReader(InputStreamReader(it)).readText()
                }
            }
            val boats: List<BoatEntity> = gson.fromJson(json, boatListType)
            for (boat in boats) {
                boatDao.addBoatById(boat.copy(id = 0))
            }
            Toast.makeText(context, "Boats restored successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to restore boats: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        boatAdapter = BoatListAdapter(
            onItemClick = { boat ->
                // Handle click on a boat: Navigate to an edit screen or show a dialog
                // You'll likely pass boat.id to the next screen/dialog
                navigateToEditBoatScreen(boat.id)
            }

        )
        binding.recyclerViewBoats.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = boatAdapter
        }
    }

    private fun observeBoatList() {
        boatViewModel.allBoats.observe(viewLifecycleOwner) { boats ->
            boatAdapter.submitList(boats) // Assuming ListAdapter for efficient updates
        } // Using observe with LiveData
    }


    private fun navigateToEditBoatScreen(boatId: Int) {
        // Use Navigation Component or FragmentTransaction to go to your edit screen
        // Pass the boatId as an argument
        val bundle = Bundle()
        bundle.putInt("boatId", boatId)
        bundle.putString("action", "edit")
        navController.navigate(
            R.id.action_maintBoatFragment_to_maintainIndividualBoatFragment,
            bundle
        )
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important for ViewBinding in Fragments
    }
}
