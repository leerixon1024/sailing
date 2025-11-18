package com.spinnaker.sailing.ui.course





import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CourseViewModel(private val courseDao: CourseDao) : ViewModel() {

    // LiveData to observe the list of all courses
    val allCourses: LiveData<List<CourseEntity>> = courseDao.getAllCourses().asLiveData()

    // Function to get a single course by its ID
    suspend fun getCourseById(courseId: Int): CourseEntity? {
        return courseDao.getCourseById(courseId)
    }

    // Function to add a new course
    fun addCourse(courseDesc: String) {
        viewModelScope.launch {
            val newCourse = CourseEntity(courseDesc = courseDesc)
            courseDao.addCourseById(newCourse)
        }
    }

    // Function to update an existing course
    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch {
            courseDao.updateCourseById(course)
        }
    }

    // Function to delete a course
    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            courseDao.deleteCourseById(course)
        }
    }

}



