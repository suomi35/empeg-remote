package com.chasinglemons.empeg

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow

open class PopupWindows(protected var mContext: Context) {
    protected var mWindow: PopupWindow = PopupWindow(mContext)
    protected var mRootView: View? = null
    protected var mBackground: Drawable? = null
    protected var mWindowManager: WindowManager

    init {
        mWindow.setTouchInterceptor(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                mWindow.dismiss()
                return@OnTouchListener true
            }
            false
        })

        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    protected fun onDismiss() {
    }

    protected fun onShow() {
    }

    protected fun preShow() {
        checkNotNull(mRootView) { "setContentView was not called with a view to display." }

        onShow()

        if (mBackground == null) mWindow.setBackgroundDrawable(BitmapDrawable())
        else mWindow.setBackgroundDrawable(mBackground)

        mWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        mWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mWindow.isTouchable = true
        mWindow.isFocusable = true
        mWindow.isOutsideTouchable = true

        mWindow.contentView = mRootView
    }

    fun setBackgroundDrawable(background: Drawable?) {
        mBackground = background
    }

    fun setContentView(root: View?) {
        mRootView = root
        mWindow.contentView = root
    }

    fun setContentView(layoutResID: Int) {
        val inflator = mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        setContentView(inflator.inflate(layoutResID, null))
    }

    fun setOnDismissListener(listener: PopupWindow.OnDismissListener?) {
        mWindow.setOnDismissListener(listener)
    }

    fun dismiss() {
        mWindow.dismiss()
    }
}