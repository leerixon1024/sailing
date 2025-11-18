package com.spinnaker.sailing.ui

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RaceTimerViewModel : ViewModel() {

    private val _currentTime = MutableLiveData<Long>()
    val currentTime: LiveData<Long> = _currentTime

    private val _isTimerRunning = MutableLiveData<Boolean>()
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning

    private val _timerFinished = MutableLiveData<Boolean>()
    val timerFinished: LiveData<Boolean> = _timerFinished

    private val _isCountingUp = MutableLiveData<Boolean>(false)
    val isCountingUp: LiveData<Boolean> = _isCountingUp

    private var countDownTimer: CountDownTimer? = null

    init {
        _isTimerRunning.value = false
        _timerFinished.value = false
    }

    fun startTimer(durationMillis: Long) {
        if (_isTimerRunning.value == true) {
            return // Timer is already running
        }

        _isTimerRunning.value = true
        _isCountingUp.value = false
        _timerFinished.value = false

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _currentTime.value = millisUntilFinished
            }

            override fun onFinish() {
                _currentTime.value = 0
                _isTimerRunning.value = false
                _timerFinished.value = true
                _isCountingUp.value = true // Start counting up
                startCountdown() // Start count-up timer
            }
        }.start()
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _currentTime.value = (Long.MAX_VALUE - millisUntilFinished)
            }

            override fun onFinish() { /* Not expected to happen */ }
        }.start()
    }


    fun stopTimer() {
        countDownTimer?.cancel()
        _isTimerRunning.value = false
    }

    fun resetTimer(durationMillis: Long) {
        stopTimer()
        _currentTime.value = durationMillis
        _timerFinished.value = false
        _isCountingUp.value = false
    }

    fun onTimerFinishHandled() {
        _timerFinished.value = false
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}
