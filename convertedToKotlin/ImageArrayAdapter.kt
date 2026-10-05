package com.chasinglemons.empeg

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView

class ImageArrayAdapter(
    context: Context, textViewResourceId: Int,
    objects: Array<CharSequence?>, ids: IntArray?, i: Int, isPro: String?
) :
    ArrayAdapter<CharSequence?>(context, textViewResourceId, objects) {
    private var index = 0
    private var resourceIds: IntArray? = null

    init {
        index = i
        resourceIds = ids
    }

    /**
     * {@inheritDoc}
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = (context as Activity).layoutInflater
        val row = inflater.inflate(R.layout.listitem, parent, false)

        val imageView = row.findViewById<View>(R.id.image) as ImageView
        imageView.setImageResource(resourceIds!![position])

        val checkedTextView = row.findViewById<View>(
            R.id.check
        ) as CheckedTextView

        checkedTextView.text = getItem(position)

        if (position == index) {
            checkedTextView.isChecked = true
        }

        return row
    }
}
