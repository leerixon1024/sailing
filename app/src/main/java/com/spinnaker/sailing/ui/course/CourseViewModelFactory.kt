package com.spinnaker.sailing.ui.course // Or wherever your factory is located

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CourseViewModelFactory(private val courseDao: CourseDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseViewModel(courseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
