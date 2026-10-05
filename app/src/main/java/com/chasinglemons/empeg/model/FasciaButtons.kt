package com.chasinglemons.empeg.model

import android.graphics.Point

/**
 * Represents a rectangle on the image.
 * Defined by its top-left corner (x, y) and its width and height.
 */
data class RectangularButton(
    val x: Float,      // X-coordinate of the top-left corner of the rectangle
    val y: Float,      // Y-coordinate of the top-left corner of the rectangle
    val width: Float,  // Width of the rectangle
    val height: Float,  // Height of the rectangle
    val onClickCommand: String,
    val onLongClickCommand: String?
) {
    // Calculated properties for convenience
    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + width
    val bottom: Float get() = y + height

    /**
     * Checks if this rectangle contains the given point.
     * The point is considered inside if it's within or on the boundary of the rectangle.
     */
    fun contains(point: Point): Boolean {
        return point.x >= this.left &&
                point.x <= this.right &&
                point.y >= this.top &&
                point.y <= this.bottom
    }
}

data class CircularButton(
    val center: Point,
    val radius: Float,
    val onClickCommand: String,
    val onLongClickCommand: String?
) {

    fun contains(point: Point): Boolean {
        val dx = point.x - this.center.x
        val dy = point.y - this.center.y
        val distanceSquared = (dx * dx) + (dy * dy)
        return distanceSquared <= (this.radius * this.radius)
    }
}