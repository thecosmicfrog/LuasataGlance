package org.thecosmicfrog.luasataglance.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class NonSwipeableViewPager(context: Context?, attrs: AttributeSet?) : ViewPager(context!!, attrs) {

    var swipingEnabled = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (swipingEnabled) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (swipingEnabled) {
            super.onInterceptTouchEvent(event)
        } else false
    }
}

