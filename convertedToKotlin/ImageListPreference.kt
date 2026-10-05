package com.chasinglemons.empeg

import android.app.AlertDialog
import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import android.widget.ListAdapter

class ImageListPreference(context: Context, attrs: AttributeSet?) :
    ListPreference(context, attrs) {
    private var resourceIds: IntArray? = null

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.ImageListPreference
        )

        val imageNames = context.resources.getStringArray(
            typedArray.getResourceId(typedArray.indexCount - 1, -1)
        )

        resourceIds = IntArray(imageNames.size)

        for (i in imageNames.indices) {
            val imageName = imageNames[i].substring(
                imageNames[i].indexOf('/') + 1,
                imageNames[i].lastIndexOf('.')
            )

            resourceIds!![i] = context.resources.getIdentifier(
                imageName,
                null, context.packageName
            )
        }

        typedArray.recycle()
    }

    /**
     * {@inheritDoc}
     */
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val index = findIndexOfValue(
            sharedPreferences.getString(
                key, "0"
            )
        )

        val listAdapter: ListAdapter = ImageArrayAdapter(
            context,
            R.layout.listitem,
            entries, resourceIds, index, sharedPreferences.getString("suomi35_empeg", "")
        )

        // Order matters.
        builder.setTitle("Select lens color")

        builder.setAdapter(listAdapter, this)
        super.onPrepareDialogBuilder(builder)
    }
}
