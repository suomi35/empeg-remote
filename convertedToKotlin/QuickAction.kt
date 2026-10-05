package com.chasinglemons.empeg

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.TextView
import kotlin.math.pow
import kotlin.math.sqrt

class QuickAction(context: Context) : PopupWindows(context) {
    private val mTrackAnim: Animation =
        AnimationUtils.loadAnimation(context, R.anim.rail)
    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var mTrack: ViewGroup? = null
    private var mListener: OnActionItemClickListener? = null
    private var mChildPos: Int
    private var animateTrack: Boolean
    override var mContext: Context
    var tabletMinimum: Double = 6.0

    init {
        mTrackAnim.interpolator = Interpolator { t ->
            val inner = (t * 1.55f) - 1.1f
            1.2f - inner * inner
        }

        setRootViewId(R.layout.quickaction)

        animateTrack = true
        mChildPos = 0
        mContext = context
    }

    fun setRootViewId(id: Int) {
        mRootView = inflater.inflate(id, null)
        mTrack = mRootView.findViewById<View>(R.id.tracks) as ViewGroup

        setContentView(mRootView)
    }

    fun animateTrack(animateTrack: Boolean) {
        this.animateTrack = animateTrack
    }

    fun addActionItem(action: ActionItem) {
        val title = action.title

        val container = inflater.inflate(R.layout.action_item, null)

        val text = container.findViewById<View>(R.id.tv_title) as TextView

        if (title != null) text.text = title
        else text.visibility = View.GONE

        val pos = mChildPos

        container.setOnClickListener {
            if (mListener != null) mListener!!.onItemClick(pos)
            dismiss()
        }

        container.isFocusable = true
        container.isClickable = true

        mTrack!!.addView(container, mChildPos + 1)
        mChildPos++
    }

    fun setOnActionItemClickListener(listener: OnActionItemClickListener?) {
        mListener = listener
    }

    fun show(anchor: View) {
        preShow()

        val location = IntArray(2)

        anchor.getLocationOnScreen(location)
        Log.i("QUICKACTION", "location[0] = " + location[0])
        Log.i("QUICKACTION", "location[1] = " + location[1])

        val anchorRect = Rect(
            location[0], location[1], location[0]
                    + anchor.width, location[1] + anchor.height
        )

        mRootView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mRootView!!.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val rootWidth = mRootView!!.measuredWidth
        val rootHeight = mRootView!!.measuredHeight

        val screenWidth = mWindowManager.defaultDisplay.width

        //Log.i("QUICKACTION","screenWidth = "+screenWidth);
        val xPos = if (isTablet) { // tablet
            ((screenWidth * .75) - (rootWidth / 2)).toInt()
        } else {
            (screenWidth - rootWidth) / 2
        }
        var yPos = anchorRect.top - rootHeight

        //		Log.i("QUICKACTION","rootHeight = "+rootHeight+", anchor.getTop() = "+anchor.getTop());
        if (rootHeight > anchor.top) {
//			Log.i("QUICKACTION","rootHeight > anchor.getTop()...");
            yPos = anchorRect.bottom
        }

        mWindow.animationStyle = R.style.Animations_PopDownMenu_Center

        //Log.i("QA","anchorRect.right = "+anchorRect.right);
        //Log.i("QA","anchorRect.top = "+anchorRect.top);
        //Log.i("QA","xPos = "+xPos);
        //Log.i("QA","yPos = "+yPos);
        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos)

        if (animateTrack) mTrack!!.startAnimation(mTrackAnim)
    }

    val isTablet: Boolean
        get() {
            try {
                // Compute screen size 
                val dm = mContext.resources.displayMetrics
                val screenWidth = dm.widthPixels / dm.xdpi
                val screenHeight = dm.heightPixels / dm.ydpi
                val size = sqrt(
                    screenWidth.toDouble().pow(2.0) + screenHeight.toDouble().pow(2.0)
                )

                // Tablet devices should have a screen size greater than 6 inches 
                return size >= tabletMinimum
            } catch (t: Throwable) {
//			Log.e("START", "Failed to compute screen size", t);
                return false
            }
        }

    interface OnActionItemClickListener {
        fun onItemClick(pos: Int)
    }
}