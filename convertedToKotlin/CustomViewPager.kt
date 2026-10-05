package com.chasinglemons.empeg

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager(context: Context, attrs: AttributeSet?) :
    ViewPager(context, attrs) {
    private var enabled = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (this.enabled) {
            return super.onTouchEvent(event)
        }

        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (this.enabled) {
            return super.onInterceptTouchEvent(event)
        }

        return false
    }

    fun setPagingEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}