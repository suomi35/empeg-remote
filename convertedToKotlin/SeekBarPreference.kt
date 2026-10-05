package com.chasinglemons.empeg

import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView

class SeekBarPreference : Preference, OnSeekBarChangeListener {
    private val TAG: String = javaClass.name

    private var mMaxValue = 100
    private var mMinValue = 0
    private var mInterval = 1
    private var mCurrentValue = 0
    private var mUnits = "sec"

    private var mStatusText: TextView? = null

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs) {
        setValuesFromXml(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        setValuesFromXml(attrs)
    }

    private fun setValuesFromXml(attrs: AttributeSet) {
        mMaxValue = attrs.getAttributeIntValue(androidns, "max", 100)
        mMinValue = attrs.getAttributeIntValue(chasinglemonsseekns, "min", 0)
        mUnits = attrs.getAttributeValue(chasinglemonsseekns, "units")
        try {
            val newInterval = attrs.getAttributeValue(chasinglemonsseekns, "interval")
            if (newInterval != null) mInterval = newInterval.toInt()
        } catch (e: Exception) {
            //Log.e(TAG, "Invalid interval value", e);
        }
    }

    override fun onCreateView(parent: ViewGroup): View? {
        super.onCreateView(parent)
        var layout: RelativeLayout? = null

        try {
            val mInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            layout =
                mInflater.inflate(R.layout.seek_bar_preference, parent, false) as RelativeLayout

            val title = layout!!.findViewById<View>(R.id.seekBarPrefTitle) as TextView
            title.text = getTitle()

            val summary = layout.findViewById<View>(R.id.seekBarPrefSummary) as TextView
            summary.text = getSummary()

            val seekBar = layout.findViewById<View>(R.id.seekBarPrefBar) as SeekBar
            seekBar.max = mMaxValue - mMinValue
            seekBar.progress = mCurrentValue - mMinValue
            seekBar.setOnSeekBarChangeListener(this)

            mStatusText = layout.findViewById<View>(R.id.seekBarPrefValue) as TextView
            mStatusText!!.text = mCurrentValue.toString()
            mStatusText!!.minimumWidth = 30

            val units = layout.findViewById<View>(R.id.seekBarPrefUnits) as TextView
            units.text = mUnits
        } catch (e: Exception) {
            //Log.e(TAG, "Error building seek bar preference", e);
        }

        return layout
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        var newValue = progress + mMinValue

        if (newValue > mMaxValue) newValue = mMaxValue
        else if (newValue < mMinValue) newValue = mMinValue
        else if (mInterval != 1 && newValue % mInterval != 0) newValue =
            Math.round((newValue.toFloat()) / mInterval) * mInterval


        // change rejected, revert to the previous value
        if (!callChangeListener(newValue)) {
            seekBar.progress = mCurrentValue - mMinValue
            return
        }

        // change accepted, store it
        mCurrentValue = newValue
        mStatusText!!.text = newValue.toString()
        persistInt(newValue)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        notifyChanged()
    }


    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        val defaultValue = ta.getInt(index, DEFAULT_VALUE)
        return defaultValue
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any) {
        if (restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue)
        } else {
            val temp = defaultValue as Int
            persistInt(temp)
            mCurrentValue = temp
        }
    }

    companion object {
        private const val androidns = "http://schemas.android.com/apk/res/android"
        private const val chasinglemonsseekns = "http://com.chasinglemons.empeg"
        private const val DEFAULT_VALUE = 50
    }
}

