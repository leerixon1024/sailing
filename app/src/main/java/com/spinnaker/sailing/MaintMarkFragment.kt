package com.spinnaker.sailing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.mark.MarkEntity
import com.spinnaker.sailing.ui.mark.MarkListAdapter
import com.spinnaker.sailing.ui.mark.MarkViewModel
import com.spinnaker.sailing.ui.mark.MarkViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class MaintMarkFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController
    private lateinit var markViewModel: MarkViewModel
    private lateinit var markDao: MarkDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_maint_mark, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        markDao = AppDatabase.getDatabase(requireContext()).MarkDao()

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_add_new).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_backup_marks).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_restore_marks).setOnClickListener(this)


        val adapter = MarkListAdapter { mark ->
            val bundle = bundleOf("markId" to mark.id)
            navController.navigate(R.id.action_maintMarkFragment_to_maintainIndividualMarkFragment, bundle)
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMark)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val factory = MarkViewModelFactory(markDao)
        markViewModel = ViewModelProvider(this, factory).get(MarkViewModel::class.java)
        markViewModel.allMarkData.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    writeMarksToUri(uri)
                }
            }
        }
    }

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    readMarksFromUri(uri)
                }
            }
        }
    }

    private suspend fun writeMarksToUri(uri: Uri) {
        val marks = withContext(Dispatchers.IO) {
             markDao.getAllMarksList()
        }

        if (marks.isEmpty()) {
            Toast.makeText(requireContext(), "No marks in the database to back up.", Toast.LENGTH_SHORT).show()
            return
        }

        val gson = Gson()
        val json = gson.toJson(marks)

        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use {
                    it.write(json.toByteArray())
                }
            }
            Toast.makeText(requireContext(), "${marks.size} marks backed up successfully.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to backup marks: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun readMarksFromUri(uri: Uri) {
        val gson = Gson()

        try {
            val jsonString = requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

            if (jsonString.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Backup file is empty or could not be read.", Toast.LENGTH_LONG).show()
                return
            }

            val typeToken = object : TypeToken<List<MarkEntity>>() {}.type
            val restoredMarks: List<MarkEntity> = gson.fromJson(jsonString, typeToken)

            var marksAdded = 0
            var marksUpdated = 0

            withContext(Dispatchers.IO) {
                for (markFromFile in restoredMarks) {
                    val existingMark = markDao.getMarkByName(markFromFile.markName)
                    if (existingMark != null) {
                        val updatedMark = existingMark.copy(
                            markLatitude = markFromFile.markLatitude,
                            markLongitude = markFromFile.markLongitude
                        )
                        markDao.updateMark(updatedMark)
                        marksUpdated++
                    } else {
                        // When adding a new mark, ensure the ID is 0 to allow Room to auto-generate it.
                        markDao.addMark(markFromFile.copy(id = 0))
                        marksAdded++
                    }
                }
            }
            Toast.makeText(requireContext(), "Restore complete. Added: $marksAdded, Updated: $marksUpdated.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Restore Error")
                    .setMessage("An error occurred during restore. Please ensure you selected a valid marks backup file.\n\nError: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun backupMarks() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "marks_backup.json")
        }
        saveFileLauncher.launch(intent)
    }

    private fun restoreMarks() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        openFileLauncher.launch(intent)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.left_btn -> navController.popBackStack()
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.button_add_new -> navController.navigate(R.id.action_maintMarkFragment_to_maintainIndividualMarkFragment)
            R.id.button_backup_marks -> backupMarks()
            R.id.button_restore_marks -> restoreMarks()
        }
    }
}