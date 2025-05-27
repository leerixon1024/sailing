package com.spinnaker.sailing

import android.content.ContentValues.TAG
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class Timer {

        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Default + job)

        private fun startCoroutineTimer(delayMillis: Long = 0, repeatMillis: Long = 0, action: () -> Unit) = scope.launch(Dispatchers.IO) {

                action()

        }

        private val timer: Job = startCoroutineTimer(delayMillis = 0, repeatMillis = 20000) {
            Log.d(TAG, "Background - tick")
            doSomethingBackground()
            scope.launch(Dispatchers.Main) {
                Log.d(TAG, "Main thread - tick")
                doSomethingMainThread()
            }
        }

    private fun doSomethingBackground() {
        TODO("Not yet implemented")
    }

    private fun doSomethingMainThread() {
        TODO("Not yet implemented")
    }

    fun startTimer() {
            timer.start()
        }

        fun cancelTimer() {
            timer.cancel()
        }
        fun setFive() {

        }
        fun setFour() {

        }
        fun setOne() {

        }
//...
    }
