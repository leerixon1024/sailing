package com.spinnaker.sailing.ui.boatperformance



import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope


import kotlinx.coroutines.launch

class BoatPerformanceViewModel(private val boatPerformanceDao: BoatPerformanceDao) : ViewModel() {

    val allPerformanceData: LiveData<List<BoatPerformanceEntity>> = boatPerformanceDao.getAllBoatPerformance().asLiveData()




    // Function to get a single boat performance by its ID
    suspend fun getSpecificBoatPerformance(boatId: Int): List<BoatPerformanceEntity> {
        return boatPerformanceDao.getSpecificBoatPerformance(boatId)
    }

    // Function to add a new boat performance
    fun addBoatPerformance(boatId: Int, trueWind: Int, tackAngle: Int, gybeAngle: Int, targetSpeedUpwind: Float, targetSpeedOffwind: Float, turnaroundTimeInSeconds: Int) {
        viewModelScope.launch {
            val newBoatPerformance = BoatPerformanceEntity(
                boatId = boatId,
                trueWind = trueWind,
                tackAngle = tackAngle,
                gybeAngle = gybeAngle,
                targetSpeedUpwind = targetSpeedUpwind,
                targetSpeedOffwind = targetSpeedOffwind,
                turnaroundTimeInSeconds = turnaroundTimeInSeconds,

            )
            boatPerformanceDao.insertBoatPerformance(newBoatPerformance)
        }
    }

    // Function to update an existing boat performance
    fun updateBoatPerformance(boatPerformance: BoatPerformanceEntity) {
        viewModelScope.launch {
            boatPerformanceDao.updateBoatPerformance(boatPerformance)
        }
    }

    // Function to delete a boat
    fun deleteBoatPerformance(boatPerformance: BoatPerformanceEntity) {
        viewModelScope.launch {
            boatPerformanceDao.deleteBoatPerformance(boatPerformance)
        }
    }

}