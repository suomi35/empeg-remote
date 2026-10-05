package com.chasinglemons.empeg

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.atan2
import kotlin.math.sqrt

class RotaryKnobView : AppCompatImageView {
    private var angle = 0f
    private var theta_old = 0f

    var width: Float = 300f
    var height: Float = 300f

    private var listener: RotaryKnobListener? = null

    interface RotaryKnobListener {
        fun onKnobChanged(arg: Int)
    }

    fun setKnobListener(l: RotaryKnobListener?) {
        listener = l
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initialize()
    }

    private fun getTheta(x: Float, y: Float): Float {
        val sx = x - (width / 2.0f)
        val sy = y - (height / 2.0f)

        val length = sqrt((sx * sx + sy * sy).toDouble()).toFloat()
        val nx = sx / length
        val ny = sy / length
        val theta = atan2(ny.toDouble(), nx.toDouble()).toFloat()

        val rad2deg = (180.0 / Math.PI).toFloat()
        val theta2 = theta * rad2deg

        return if (theta2 < 0) theta2 + 360.0f else theta2
    }

    fun initialize() {
        //      this.setImageResource(R.drawable.dial);

        setOnTouchListener { v, event -> // TODO Auto-generated method stub
            val action = event.action
            val actionCode = action and MotionEvent.ACTION_MASK
            if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
                val x = event.getX(0)
                val y = event.getY(0)
                theta_old = getTheta(x, y)
            } else if (actionCode == MotionEvent.ACTION_MOVE) {
                invalidate()

                val x = event.getX(0)
                val y = event.getY(0)

                val theta = getTheta(x, y)
                val delta_theta = theta - theta_old

                theta_old = theta

                val direction = if (delta_theta > 0) 1 else -1
                angle += (3 * direction).toFloat()

                notifyListener(direction)
            }
            true
        }
    }

    private fun notifyListener(arg: Int) {
        if (null != listener) listener!!.onKnobChanged(arg)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(angle, 150f, 150f)
        super.onDraw(c)
    }
}