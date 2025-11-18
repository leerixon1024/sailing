package com.spinnaker.sailing

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.spinnaker.sailing.ui.course.CourseEntity

import kotlinx.coroutines.launch
import com.spinnaker.sailing.databinding.FragmentMaintCourseBinding // ViewBinding

import com.spinnaker.sailing.ui.course.CourseListAdapter

import com.spinnaker.sailing.ui.course.CourseViewModel
import com.spinnaker.sailing.ui.course.CourseViewModelFactory


class MaintCourseFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController

    // Assuming you are using ViewBinding
    private var _binding: FragmentMaintCourseBinding? = null
    private val binding get() = _binding!!

    // Get the CourseDao instance from your Room Database
    // This is a simplified example; you'd typically use Hilt or manual DI
    private val courseDao by lazy {
        AppDatabase.getDatabase(requireContext()).CourseDao()
    }

    private val courseViewModel: CourseViewModel by viewModels {
        CourseViewModelFactory(courseDao)
    }
    private lateinit var newCourseDescriptionEditText: EditText
    private lateinit var courseAdapter: CourseListAdapter // Your RecyclerView adapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMaintCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_add_new).setOnClickListener(this)
        newCourseDescriptionEditText = view.findViewById(R.id.new_course_description)
        newCourseDescriptionEditText.filters = arrayOf(InputFilter.AllCaps())
        // load all courses from database
        setupRecyclerView()
        observeCourseList()

    }

    override fun onClick(v: View?) {
        val bundle = Bundle()
        when (v!!.id) {
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.action_maintCourseFragment_to_maintainIndividualCourseFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.button_add_new -> {
                val newCourseDescription = binding.newCourseDescription.text.toString()
                lifecycleScope.launch {
                    if (newCourseDescription.isNotEmpty()) {
                        addNewCourseToDatabase()
                    } else {
                        Toast.makeText(
                            context,
                            "Please enter a course description",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        courseAdapter = CourseListAdapter(
            onItemClick = { course ->
                // Handle click on a course: Navigate to an edit screen or show a dialog
                // You'll likely pass course.id to the next screen/dialog
                navigateToEditCourseScreen(course.id)
            }

        )
        binding.recyclerViewCourse.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = courseAdapter
        }
    }

    private fun observeCourseList() {
        courseViewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            courseAdapter.submitList(courses) // Assuming ListAdapter for efficient updates
        } // Using observe with LiveData
    }


    private fun navigateToEditCourseScreen(courseId: Int) {
        // Use Navigation Component or FragmentTransaction to go to your edit screen
        // Pass the courseId as an argument
        val bundle = Bundle()
        bundle.putInt("courseId", courseId)
        bundle.putString("action", "edit")
        navController.navigate(
            R.id.action_maintCourseFragment_to_maintainIndividualCourseFragment,
            bundle
        )
    }

    private suspend fun addNewCourseToDatabase() {
        val newCourseDescription = newCourseDescriptionEditText.text.toString().trim()
        val newCourse = CourseEntity(
            0,
            courseDesc = newCourseDescription
        )

        val result = courseDao.addCourseById(newCourse)
        if (result != -1L) {
            Toast.makeText(context, "Course added successfully", Toast.LENGTH_SHORT).show()
            newCourseDescriptionEditText.text.clear() // Clear the EditText after adding
        } else {
            Toast.makeText(context, "Error adding course", Toast.LENGTH_SHORT).show()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important for ViewBinding in Fragments
    }
}