package com.spinnaker.sailing.ui.boat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BoatViewModelFactory(private val boatDao: BoatDao) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BoatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BoatViewModel(boatDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}