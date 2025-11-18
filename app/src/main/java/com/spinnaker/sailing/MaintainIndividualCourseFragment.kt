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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.spinnaker.sailing.ui.course.CourseDao
import com.spinnaker.sailing.ui.course.CourseEntity

import com.spinnaker.sailing.ui.coursemark.CourseMarkListAdapter
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkEntity
import com.spinnaker.sailing.ui.coursemark.CourseMarkWithMark

import com.spinnaker.sailing.ui.mark.MarkListAdapter
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.mark.MarkEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class MaintainIndividualCourseFragment : Fragment(), View.OnClickListener {


    private lateinit var courseDescEditText: EditText

    private lateinit var courseMarkDao: CourseMarkDao

    private lateinit var courseDao: CourseDao

    private lateinit var markDao: MarkDao
    private var currentCourseId: Int = -1
    private var currentAction: String? = null
    private lateinit var marksRecyclerView: RecyclerView
    private lateinit var markListAdapter: MarkListAdapter


    private lateinit var courseMarkRecyclerView: RecyclerView
    private lateinit var courseMarkListAdapter: CourseMarkListAdapter
    lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_maintain_individual_course, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access arguments here
        Log.d("MIMF_DEBUG", "Arguments object: $arguments")

        arguments?.let { bundle ->
            currentCourseId =
                bundle.getInt("courseId", -1) // -1 is a default value if "courseId" isn't found
                }
        Log.d("MIMF", "Came into edit Course ID: $currentCourseId, Action: $currentAction")
        // Load course details if in edit mode


    }

    /**
     * Called when the fragment's view has been created.
     * This method initializes the UI elements, sets up RecyclerViews and their adapters,
     * and loads data if the fragment is in "edit" mode with a valid course ID.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // Initialize RecyclerViews and their adapters
        marksRecyclerView = view.findViewById(R.id.recyclerViewAllMarks)
        marksRecyclerView.layoutManager = LinearLayoutManager(context)
        courseMarkRecyclerView = view.findViewById(R.id.recyclerViewCourseMarks)
        courseMarkRecyclerView.layoutManager = LinearLayoutManager(context)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonDeleteCourse).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonUpdateCompleted).setOnClickListener(this)

        courseDescEditText = view.findViewById(R.id.textViewCourseDescription)
        courseDescEditText.filters = arrayOf(InputFilter.AllCaps())

        courseDao = AppDatabase.getDatabase(requireContext()).CourseDao() // Initialize DAO

        if (currentCourseId != -1) {
            lifecycleScope.launch {
                val course = courseDao.getCourseById(currentCourseId)
                course?.let {
                    courseDescEditText.setText(it.courseDesc)
                }
            }
        }
        markDao = AppDatabase.getDatabase(requireContext()).MarkDao() // Initialize DAO

        markListAdapter = MarkListAdapter { markItem ->
            // This is your mark click listener.
            // 'courseMarkItem' is the CourseMarkWithName object that was clicked.
            // Add your logic here, e.g., navigate to a detail screen, show a Toast, etc.
            Log.d("MarkClick", "Clicked on: ${markItem.markName}")
            Toast.makeText(context, "Clicked: ${markItem.markName}", Toast.LENGTH_SHORT)
                .show()

            // add the selected mark to the coursemark
            lifecycleScope.launch {
                if (currentCourseId != -1 && markItem.id != 0) {
                    // Get the highest current courseMarkSequence for this course
                    val highestSequence = courseMarkDao.getHighestCourseMarkSequence(currentCourseId) ?: -1 // Default to -1 if no marks yet
                    val newSequence = highestSequence + 1
                    val newCourseMark = CourseMarkEntity(id= 0, courseMarkSequence = newSequence, courseId = currentCourseId, markId = markItem.id) // Changed here
                    courseMarkDao.insert(newCourseMark)
                    loadMarksForCourse(currentCourseId) // Refresh the list of marks for the course
                }
            }
        }
        marksRecyclerView.adapter = markListAdapter



        courseMarkDao = AppDatabase.getDatabase(requireContext()).CourseMarkDao() // Initialize DAO

        courseMarkListAdapter = CourseMarkListAdapter { courseMarkItem ->
            // This is your click listener.
            // 'courseMarkItem' is the CourseMarkWithName object that was clicked.
            // Add your logic here, e.g., navigate to a detail screen, show a Toast, etc.
            Log.d("CourseMarkClick", "Clicked on: ${courseMarkItem.markName}")
            Toast.makeText(context, "Clicked: ${courseMarkItem.markName}", Toast.LENGTH_SHORT)
                .show()
            // Delete the clicked courseMark from the database
            lifecycleScope.launch {
                courseMarkDao.deleteSingleMark(courseMarkItem.courseMark.id)
                loadMarksForCourse(currentCourseId) // Refresh the list
            }
        }
        courseMarkRecyclerView.adapter = courseMarkListAdapter

        courseDao = AppDatabase.getDatabase(requireContext()).CourseDao() // Initialize DAO



        Log.d("MIMF", "just before loadCourseData ID: $currentCourseId, Action: $currentAction")
        lifecycleScope.launch {
            loadAllMarks()
            loadMarksForCourse(currentCourseId)
        }

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.maintCourseFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.buttonUpdateCompleted -> navController.navigate(R.id.maintCourseFragment)

            R.id.buttonDeleteCourse -> {


                lifecycleScope.launch {
                deleteCourseInDatabase()
                        }


            }

        }
    }

    private fun validateEntries(): Boolean {

        if (courseDescEditText.text.toString().trim().isEmpty()) {
            courseDescEditText.error = "Course Description cannot be empty"
            return false
        }

        // Add more validations as needed for other fields
        return true
    }


    private suspend fun deleteCourseInDatabase() {
        if (currentCourseId != -1) {
            val courseToDelete = CourseEntity(
                id = currentCourseId, /* other fields can be default/dummy if not used by delete */
                courseDesc = "",
                )
            val rowsAffected = courseDao.deleteCourseById(courseToDelete)
            if (rowsAffected > 0) {
                // Delete all course marks associated with this courseId
                courseMarkDao.deleteSingleCourse(currentCourseId)
                Toast.makeText(context, "Course deleted successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack() // Go back after deleting
            } else {
                Toast.makeText(
                    context,
                    "Error deleting course or course not found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "No course selected for deletion", Toast.LENGTH_SHORT).show()
        }
    }

//need to delete matching course marks
    // Or .first() if you're sure it won't be empty or want an exception

    private suspend fun loadAllMarks() {
        Toast.makeText(context, "Loading all Marks", Toast.LENGTH_SHORT).show()

        // Collect the first list emitted by the Flow
        val allMarksList: List<MarkEntity>? =
            markDao.getAllMarks().firstOrNull() // Collect from the Flow

        if (allMarksList != null) {
            // Update the adapter with the names of all marks
            markListAdapter.submitList(allMarksList)
            if (allMarksList.isEmpty()) {
                Toast.makeText(context, "No marks found in the database.", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            // Handle the case where the flow might be empty or completes without emission
            markListAdapter.submitList(emptyList())
            Toast.makeText(
                context,
                "Could not load marks or database is empty.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun loadMarksForCourse(courseId: Int) {
        if (courseId != -1) {
            // Retrieve all entries in CourseMark with matching courseId
            val courseMarksList = AppDatabase.getDatabase(requireContext()).CourseMarkDao()
                .getCourseMarksWithMark(courseId)
            // Assuming MarksAdapter has a method to update its data
            courseMarkListAdapter.submitList(courseMarksList) // Display mark IDs or fetch mark details
            if (courseMarksList.isEmpty()) {
                Toast.makeText(context, "No marks found for this course.", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            courseMarkListAdapter.submitList(emptyList()) // Clear the list if no course is selected
        }
    }

}
