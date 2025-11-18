package com.spinnaker.sailing

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ScrollView
import kotlin.math.abs

class GestureConsumingScrollView(context: Context, attrs: AttributeSet?) : ScrollView(context, attrs) {

    private var gestureDetector: GestureDetector? = null

    fun setGestureDetector(gestureDetector: GestureDetector) {
        this.gestureDetector = gestureDetector
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Let the gesture detector handle the touch event first
        if (gestureDetector?.onTouchEvent(ev!!) == true) {
            return true // Gesture detector consumed the event
        }
        return super.onInterceptTouchEvent(ev)
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        // This ensures the gesture detector gets a chance to process the event
        gestureDetector?.onTouchEvent(ev!!)
        return super.onTouchEvent(ev)
    }
}
