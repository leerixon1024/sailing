package com.spinnaker.sailing.ui.mark

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.spinnaker.sailing.genclasses.ddmToDecimal // Added import
import kotlinx.coroutines.launch

class MarkViewModel(private val markDao: MarkDao) : ViewModel() {

    val allMarkData: LiveData<List<MarkEntity>> = markDao.getAllMarks().asLiveData()
    // Function to get a single mark by its ID
    suspend fun getMarkById(markId: Int): MarkEntity? {
        return markDao.getMarkById(markId)
    }

    // Function to add a new mark - MODIFIED
    fun addMark(
        markName: String, // markNumId parameter removed
        latDegrees: Int,
        latDecimalMinutes: Double,
        latHemisphere: String,
        lonDegrees: Int,
        lonDecimalMinutes: Double,
        lonHemisphere: String
    ) {
        viewModelScope.launch {
            val markLatitude = ddmToDecimal(latDegrees, latDecimalMinutes, latHemisphere)
            val markLongitude = ddmToDecimal(lonDegrees, lonDecimalMinutes, lonHemisphere)
            val newMark = MarkEntity(
                // markNumId assignment removed
                markName = markName,
                markLatitude = markLatitude,
                markLongitude = markLongitude
            )
            markDao.addMark(newMark)
        }
    }

    // Function to update an existing mark
    fun updateMark(mark: MarkEntity) {
        viewModelScope.launch {
            markDao.updateMark(mark)
        }
    }

    // Function to delete a mark
    fun deleteMark(mark: MarkEntity) {
        viewModelScope.launch {
            markDao.deleteMark(mark)
        }
    }
}
