package com.spinnaker.sailing.ui.mark
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class MarkViewModelFactory(private val markDao: MarkDao) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarkViewModel(markDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}