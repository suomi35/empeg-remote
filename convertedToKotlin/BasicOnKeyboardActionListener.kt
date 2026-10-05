package com.chasinglemons.empeg

import android.app.Activity
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener
import android.view.KeyEvent

class BasicOnKeyboardActionListener(private val mTargetActivity: Activity?) :
    OnKeyboardActionListener {
    override fun swipeUp() {
        // TODO Auto-generated method stub
    }

    override fun swipeRight() {
        // TODO Auto-generated method stub
    }

    override fun swipeLeft() {
        // TODO Auto-generated method stub
    }

    override fun swipeDown() {
        // TODO Auto-generated method stub
    }

    override fun onText(text: CharSequence) {
        // TODO Auto-generated method stub
    }

    override fun onRelease(primaryCode: Int) {
        // TODO Auto-generated method stub
    }

    override fun onPress(primaryCode: Int) {
        // TODO Auto-generated method stub
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray) {
        val eventTime = System.currentTimeMillis()
        val event = KeyEvent(
            eventTime, eventTime,
            KeyEvent.ACTION_DOWN, primaryCode, 0, 0, 0, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        )

        mTargetActivity!!.dispatchKeyEvent(event)
    }
}