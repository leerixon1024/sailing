package com.spinnaker.sailing.ui.boat





import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BoatViewModel(private val boatDao: BoatDao) : ViewModel() {

    // LiveData to observe the list of all boats
    val allBoats: LiveData<List<BoatEntity>> = boatDao.getAllBoats().asLiveData()

    // Function to get a single boat by its ID
    suspend fun getBoatById(boatId: Int): BoatEntity? {
        return boatDao.getBoatById(boatId)
    }

    // Function to add a new boat
    fun addBoat(boatName: String, pHRF: Int, boatLength: Int, boatMake: String, boatModel: String) {
        viewModelScope.launch {
            val newBoat = BoatEntity(boatName = boatName, pHRF = pHRF, lOA = boatLength, boatMake = boatMake, boatModelName = boatModel)
            boatDao.addBoatById(newBoat)
        }
    }

    // Function to update an existing boat
    fun updateBoat(boat: BoatEntity) {
        viewModelScope.launch {
            boatDao.updateBoatById(boat)
        }
    }

    // Function to delete a boat
    fun deleteBoat(boat: BoatEntity) {
        viewModelScope.launch {
            boatDao.deleteBoatById(boat)
        }
    }



}