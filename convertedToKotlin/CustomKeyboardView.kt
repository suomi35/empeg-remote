package com.chasinglemons.empeg

import android.content.Context
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.animation.Animation

class CustomKeyboardView(context: Context?, attrs: AttributeSet?) :
    KeyboardView(context, attrs) {
    fun showWithAnimation(animation: Animation) {
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                // TODO Auto-generated method stub
            }

            override fun onAnimationRepeat(animation: Animation) {
                // TODO Auto-generated method stub
            }

            override fun onAnimationEnd(animation: Animation) {
                visibility = VISIBLE
            }
        })

        setAnimation(animation)
    }
}