package com.spinnaker.sailing

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation


var timerStartValueInSec: Long = 300000
private lateinit var countDown: CountDownTimer



class StopwatchFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stopwatch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.timer1_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.timer4_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.timer5_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.starttimer_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.stoptimer_btn).setOnClickListener(this)

    }

    override fun onClick(v: View?) {

        when (v!!.id) {
            R.id.left_btn -> {
                navController.popBackStack()
            }

            R.id.right_btn -> {
                navController.navigate(R.id.action_stopwatchFragment_to_windAngleTurnaroundFragment)
            }

            R.id.timer1_btn -> {
                requireView().findViewById<TextView>(R.id.timer_view).text = getString(R.string.min1)
                timerStartValueInSec = 60000

            }

            R.id.timer4_btn -> {
                requireView().findViewById<TextView>(R.id.timer_view).text =
                    getString(R.string.min4)
                timerStartValueInSec = 240000
            }

            R.id.timer5_btn -> {
                requireView().findViewById<TextView>(R.id.timer_view).text = getString(R.string.min5)
                timerStartValueInSec = 300000
            }

            R.id.starttimer_btn -> {

                countDown = object :
                    CountDownTimer(timerStartValueInSec, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val minutesToGo: Long = millisUntilFinished / 60000
                        val secondsToGo: Long = (millisUntilFinished ) / 1000
                        val secondsRounded: Long = secondsToGo - minutesToGo*60
                        val secondsEdited: String = secondsRounded.toString()
                        requireView().findViewById<TextView>(R.id.timer_view).text =
                            minutesToGo.toString() + " : " + String.format("%02d", secondsRounded)
                    }

                    override fun onFinish() {
                        requireView().findViewById<TextView>(R.id.timer_view).text = getString(R.string.min0)
                    }
                }.start()
            }

            R.id.stoptimer_btn -> {
                requireView().findViewById<TextView>(R.id.timer_view).text = getString(R.string.min5)
                timerStartValueInSec = 300000
                countDown.cancel()

            }
        }
    }


}








