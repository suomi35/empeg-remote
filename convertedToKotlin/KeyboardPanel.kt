package com.chasinglemons.empeg

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class KeyboardPanel(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {
    /**
     * Callback invoked when the panel is opened/closed.
     */
    interface OnPanelListener {
        /**
         * Invoked when the panel becomes fully closed.
         */
        fun onPanelClosed(panel: KeyboardPanel?)

        /**
         * Invoked when the panel becomes fully opened.
         */
        fun onPanelOpened(panel: KeyboardPanel?)
    }

    private var mIsShrinking = false
    private val mPosition: Int
    private val mDuration: Int
    private val mLinearFlying: Boolean
    private var mHandleId: Int
    private var mContentId: Int
    private val mOpenedHandle: Drawable?
    private var mTrackX = 0f
    private var mTrackY = 0f
    private var mVelocity = 0f

    private enum class State {
        ABOUT_TO_ANIMATE,
        ANIMATING,
        READY,
        TRACKING,
        FLYING,
    }

    private var mState: State
    private var mInterpolator: Interpolator? = null
    private val mGestureDetector: GestureDetector
    private var mContentHeight = 0
    private var mContentWidth = 0
    private val mOrientation: Int
    private var mWeight: Float
    private val mGestureListener: PanelOnGestureListener
    private var mBringToFront = false

    /**
     * Sets the listener that receives a notification when the panel becomes open/close.
     *
     * @param onPanelListener The listener to be notified when the panel is opened/closed.
     */
    fun setOnPanelListener(onPanelListener: OnPanelListener?) {
        panelListener = onPanelListener
    }


    /**
     * Sets the acceleration curve for panel's animation.
     *
     * @param i The interpolator which defines the acceleration curve
     */
    fun setInterpolator(i: Interpolator?) {
        mInterpolator = i
    }

    /**
     * Set the opened state of KeyboardPanel.
     *
     * @param open True if KeyboardPanel is to be opened, false if KeyboardPanel is to be closed.
     * @param animate True if use animation, false otherwise.
     *
     * @return True if operation was performed, false otherwise.
     */
    fun setOpen(open: Boolean, animate: Boolean): Boolean {
        if (mState == State.READY && isOpen xor open) {
            mIsShrinking = !open
            if (animate) {
                mState = State.ABOUT_TO_ANIMATE
                if (!mIsShrinking) {
                    // this could make flicker so we test mState in dispatchDraw()
                    // to see if is equal to ABOUT_TO_ANIMATE
                    content!!.visibility = VISIBLE
                }
                post(startAnimation)
            } else {
                content!!.visibility =
                    if (open) VISIBLE else GONE
                postProcess()
            }
            return true
        }
        return false
    }

    val isOpen: Boolean
        /**
         * Returns the opened status for KeyboardPanel.
         *
         * @return True if KeyboardPanel is opened, false otherwise.
         */
        get() = content!!.visibility == VISIBLE

    override fun onFinishInflate() {
        super.onFinishInflate()
        handle = findViewById(mHandleId)
        if (handle == null) {
            val name = resources.getResourceEntryName(mHandleId)
            throw RuntimeException("Your KeyboardPanel must have a child View whose id attribute is 'R.id.$name'")
        }
        handle!!.setOnTouchListener(touchListener)
        handle!!.setOnClickListener(clickListener)

        content = findViewById(mContentId)
        if (content == null) {
            val name = resources.getResourceEntryName(mHandleId)
            throw RuntimeException("Your KeyboardPanel must have a child View whose id attribute is 'R.id.$name'")
        }

        // reposition children
        removeView(handle)
        removeView(content)
        if (mPosition == TOP || mPosition == LEFT) {
            addView(content)
            addView(handle)
        } else {
            addView(handle)
            addView(content)
        }

        if (mClosedHandle != null) {
            handle!!.setBackgroundDrawable(mClosedHandle)
        }
        content!!.isClickable = true
        content!!.visibility = GONE
        if (mWeight > 0) {
            val params = content!!.layoutParams
            if (mOrientation == VERTICAL) {
                params.height = ViewGroup.LayoutParams.FILL_PARENT
            } else {
                params.width = ViewGroup.LayoutParams.FILL_PARENT
            }
            content!!.layoutParams = params
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val parent = parent
        if (parent != null && parent is FrameLayout) {
            mBringToFront = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (mWeight > 0 && content!!.visibility == VISIBLE) {
            val parent = parent as View
            if (parent != null) {
                if (mOrientation == VERTICAL) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        (parent.height * mWeight).toInt(),
                        MeasureSpec.EXACTLY
                    )
                } else {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        (parent.width * mWeight).toInt(),
                        MeasureSpec.EXACTLY
                    )
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        mContentWidth = content!!.width
        mContentHeight = content!!.height
    }

    override fun dispatchDraw(canvas: Canvas) {
        //		String name = getResources().getResourceEntryName(getId());
        //		Log.d(TAG, name + " ispatchDraw " + mState);
        // this is why 'mState' was added:
        // avoid flicker before animation start
        if (mState == State.ABOUT_TO_ANIMATE && !mIsShrinking) {
            var delta = if (mOrientation == VERTICAL) mContentHeight else mContentWidth
            if (mPosition == LEFT || mPosition == TOP) {
                delta = -delta
            }
            if (mOrientation == VERTICAL) {
                canvas.translate(0f, delta.toFloat())
            } else {
                canvas.translate(delta.toFloat(), 0f)
            }
        }
        if (mState == State.TRACKING || mState == State.FLYING) {
            canvas.translate(mTrackX, mTrackY)
        }
        super.dispatchDraw(canvas)
    }

    private fun ensureRange(v: Float, min: Int, max: Int): Float {
        var v = v
        v = max(v.toDouble(), min.toDouble()).toFloat()
        v = min(v.toDouble(), max.toDouble()).toFloat()
        return v
    }

    var touchListener: OnTouchListener = object : OnTouchListener {
        var initX: Int = 0
        var initY: Int = 0
        var setInitialPosition: Boolean = false
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (mState == State.ANIMATING) {
                // we are animating
                return false
            }
            //			Log.d(TAG, "state: " + mState + " x: " + event.getX() + " y: " + event.getY());
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) {
                if (mBringToFront) {
                    bringToFront()
                }
                initX = 0
                initY = 0
                if (content!!.visibility == GONE) {
                    // since we may not know content dimensions we use factors here
                    if (mOrientation == VERTICAL) {
                        initY = if (mPosition == TOP) -1 else 1
                    } else {
                        initX = if (mPosition == LEFT) -1 else 1
                    }
                }
                setInitialPosition = true
            } else {
                if (setInitialPosition) {
                    // now we know content dimensions, so we multiply factors...
                    initX *= mContentWidth
                    initY *= mContentHeight
                    // ... and set initial panel's position
                    mGestureListener.setScroll(initX, initY)
                    setInitialPosition = false
                    // for offsetLocation we have to invert values
                    initX = -initX
                    initY = -initY
                }
                // offset every ACTION_MOVE & ACTION_UP event 
                event.offsetLocation(initX.toFloat(), initY.toFloat())
            }
            if (!mGestureDetector.onTouchEvent(event)) {
                if (action == MotionEvent.ACTION_UP) {
                    // tup up after scrolling
                    post(startAnimation)
                }
            }
            return false
        }
    }

    var clickListener: OnClickListener = OnClickListener {
        if (mBringToFront) {
            bringToFront()
        }
        if (initChange()) {
            post(startAnimation)
        }
    }

    fun initChange(): Boolean {
        if (mState != State.READY) {
            // we are animating or just about to animate
            return false
        }
        mState = State.ABOUT_TO_ANIMATE
        mIsShrinking = content!!.visibility == VISIBLE
        if (!mIsShrinking) {
            // this could make flicker so we test mState in dispatchDraw()
            // to see if is equal to ABOUT_TO_ANIMATE
            content!!.visibility = VISIBLE
        }
        return true
    }

    var startAnimation: Runnable =
        Runnable { // this is why we post this Runnable couple of lines above:
            // now its safe to use mContent.getHeight() && mContent.getWidth()
            val animation: TranslateAnimation
            var fromXDelta = 0
            var toXDelta = 0
            var fromYDelta = 0
            var toYDelta = 0
            if (mState == State.FLYING) {
                mIsShrinking =
                    (mPosition == TOP || mPosition == LEFT) xor (mVelocity > 0)
            }
            var calculatedDuration: Int
            if (mOrientation == VERTICAL) {
                val height = mContentHeight
                if (!mIsShrinking) {
                    fromYDelta = if (mPosition == TOP) -height else height
                } else {
                    toYDelta = if (mPosition == TOP) -height else height
                }
                if (mState == State.TRACKING) {
                    if (abs((mTrackY - fromYDelta).toDouble()) < abs((mTrackY - toYDelta).toDouble())) {
                        mIsShrinking = !mIsShrinking
                        toYDelta = fromYDelta
                    }
                    fromYDelta = mTrackY.toInt()
                } else if (mState == State.FLYING) {
                    fromYDelta = mTrackY.toInt()
                }
                // for FLYING events we calculate animation duration based on flying velocity
                // also for very high velocity make sure duration >= 20 ms
                if (mState == State.FLYING && mLinearFlying) {
                    calculatedDuration =
                        (1000 * abs(((toYDelta - fromYDelta) / mVelocity).toDouble())).toInt()
                    calculatedDuration = max(calculatedDuration.toDouble(), 20.0).toInt()
                } else {
                    calculatedDuration =
                        (mDuration * abs((toYDelta - fromYDelta).toDouble()) / mContentHeight).toInt()
                }
            } else {
                val width = mContentWidth
                if (!mIsShrinking) {
                    fromXDelta = if (mPosition == LEFT) -width else width
                } else {
                    toXDelta = if (mPosition == LEFT) -width else width
                }
                if (mState == State.TRACKING) {
                    if (abs((mTrackX - fromXDelta).toDouble()) < abs((mTrackX - toXDelta).toDouble())) {
                        mIsShrinking = !mIsShrinking
                        toXDelta = fromXDelta
                    }
                    fromXDelta = mTrackX.toInt()
                } else if (mState == State.FLYING) {
                    fromXDelta = mTrackX.toInt()
                }
                // for FLYING events we calculate animation duration based on flying velocity
                // also for very high velocity make sure duration >= 20 ms
                if (mState == State.FLYING && mLinearFlying) {
                    calculatedDuration =
                        (1000 * abs(((toXDelta - fromXDelta) / mVelocity).toDouble())).toInt()
                    calculatedDuration = max(calculatedDuration.toDouble(), 20.0).toInt()
                } else {
                    calculatedDuration =
                        (mDuration * abs((toXDelta - fromXDelta).toDouble()) / mContentWidth).toInt()
                }
            }

            mTrackY = 0f
            mTrackX = mTrackY
            if (calculatedDuration == 0) {
                mState = State.READY
                if (mIsShrinking) {
                    content!!.visibility = GONE
                }
                postProcess()
                return@Runnable
            }

            animation = TranslateAnimation(
                fromXDelta.toFloat(),
                toXDelta.toFloat(),
                fromYDelta.toFloat(),
                toYDelta.toFloat()
            )
            animation.duration = calculatedDuration.toLong()
            animation.setAnimationListener(animationListener)
            if (mState == State.FLYING && mLinearFlying) {
                animation.interpolator = LinearInterpolator()
            } else if (mInterpolator != null) {
                animation.interpolator = mInterpolator
            }
            startAnimation(animation)
        }

    private val animationListener: Animation.AnimationListener =
        object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation) {
                mState = State.READY
                if (mIsShrinking) {
                    content!!.visibility = GONE
                }
                postProcess()
            }

            override fun onAnimationRepeat(animation: Animation) {
            }

            override fun onAnimationStart(animation: Animation) {
                mState = State.ANIMATING
            }
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Panel)
        mDuration =
            a.getInteger(R.styleable.Panel_animationDuration, 750) // duration defaults to 750 ms
        mPosition = a.getInteger(R.styleable.Panel_position, BOTTOM) // position defaults to BOTTOM
        mLinearFlying =
            a.getBoolean(R.styleable.Panel_linearFlying, false) // linearFlying defaults to false
        mWeight = a.getFraction(R.styleable.Panel_weight, 0, 1, 0.0f) // weight defaults to 0.0
        if (mWeight < 0 || mWeight > 1) {
            mWeight = 0.0f
            Log.w(TAG, a.positionDescription + ": weight must be > 0 and <= 1")
        }
        mOpenedHandle = a.getDrawable(R.styleable.Panel_openedHandle)
        mClosedHandle = a.getDrawable(R.styleable.Panel_closedHandle)

        var e: RuntimeException? = null
        mHandleId = a.getResourceId(R.styleable.Panel_handle, 0)
        if (mHandleId == 0) {
            e = IllegalArgumentException(
                a.positionDescription +
                        ": The handle attribute is required and must refer to a valid child."
            )
        }
        mContentId = a.getResourceId(R.styleable.Panel_content, 0)
        if (mContentId == 0) {
            e = IllegalArgumentException(
                a.positionDescription +
                        ": The content attribute is required and must refer to a valid child."
            )
        }
        a.recycle()

        if (e != null) {
            throw e
        }
        mOrientation = if (mPosition == TOP || mPosition == BOTTOM) VERTICAL else HORIZONTAL
        orientation = mOrientation
        mState = State.READY
        mGestureListener = PanelOnGestureListener()
        mGestureDetector = GestureDetector(mGestureListener)
        mGestureDetector.setIsLongpressEnabled(false)

        // i DON'T really know why i need this...
        isBaselineAligned = false
    }

    private fun postProcess() {
        if (mIsShrinking && mClosedHandle != null) {
            handle!!.setBackgroundDrawable(mClosedHandle)
        } else if (!mIsShrinking && mOpenedHandle != null) {
            handle!!.setBackgroundDrawable(mOpenedHandle)
        }
        // invoke listener if any
        if (panelListener != null) {
            if (mIsShrinking) {
                panelListener!!.onPanelClosed(this@KeyboardPanel)
            } else {
                panelListener!!.onPanelOpened(this@KeyboardPanel)
            }
        }
    }

    internal inner class PanelOnGestureListener : GestureDetector.OnGestureListener {
        var scrollY: Float = 0f
        var scrollX: Float = 0f
        fun setScroll(initScrollX: Int, initScrollY: Int) {
            scrollX = initScrollX.toFloat()
            scrollY = initScrollY.toFloat()
        }

        override fun onDown(e: MotionEvent): Boolean {
            scrollY = 0f
            scrollX = scrollY
            initChange()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            mState = State.FLYING
            mVelocity = if (mOrientation == VERTICAL) velocityY else velocityX
            post(startAnimation)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            // not used
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            mState = State.TRACKING
            var tmpY = 0f
            var tmpX = 0f
            if (mOrientation == VERTICAL) {
                scrollY -= distanceY
                tmpY = if (mPosition == TOP) {
                    ensureRange(scrollY, -mContentHeight, 0)
                } else {
                    ensureRange(scrollY, 0, mContentHeight)
                }
            } else {
                scrollX -= distanceX
                tmpX = if (mPosition == LEFT) {
                    ensureRange(scrollX, -mContentWidth, 0)
                } else {
                    ensureRange(scrollX, 0, mContentWidth)
                }
            }
            if (tmpX != mTrackX || tmpY != mTrackY) {
                mTrackX = tmpX
                mTrackY = tmpY
                invalidate()
            }
            return true
        }

        override fun onShowPress(e: MotionEvent) {
            // not used
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // not used
            return false
        }
    }

    companion object {
        private const val TAG = "KeyboardPanel"

        var handle: View? = null
            /**
             * Gets KeyboardPanel's mHandle
             *
             * @return KeyboardPanel's mHandle
             */
            get() = Companion.field
            private set
        var content: View? = null
            /**
             * Gets KeyboardPanel's mContent
             *
             * @return KeyboardPanel's mContent
             */
            get() = Companion.field
            private set
        private var mClosedHandle: Drawable?
        private var panelListener: OnPanelListener? = null

        const val TOP: Int = 0
        const val BOTTOM: Int = 1
        const val LEFT: Int = 2
        const val RIGHT: Int = 3

        fun setClosed() {
            val errorHandler = Handler(Looper.getMainLooper())
            errorHandler.post {
                content!!.visibility = GONE
            }
        }
    }
}
