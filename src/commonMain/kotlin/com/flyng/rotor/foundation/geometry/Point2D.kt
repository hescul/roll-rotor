package com.flyng.rotor.foundation.geometry

/**
 * A 2D point consists of 2 components: x-coordinate and y-coordinate. [Point2D] always returns the coordinate
 * components as [Float].
 * @param x the x-coordinate.
 * @param y the y-coordinate.
 */
class Point2D(x: Number, y: Number) {

    /**
     * x-coordinate value of this point.
     */
    val x = x.toFloat()

    /**
     * y-coordinate value of this point.
     */
    val y = y.toFloat()

    operator fun component1() = x

    operator fun component2() = y
}