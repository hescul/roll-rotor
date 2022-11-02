package com.flyng.rotor.foundation.geometry

/**
 * A 2D curve is a set of 2D points described by an [implicit equation](https://en.wikipedia.org/wiki/Implicit_function)
 * To construct a 2D curve, provide a relation equation between x and y. If an arbitrary [Point2D] whose coordinates
 * evaluated to 0 by the specified equation, the point is considered belong to the curve.
 */
class Curve2D(private val func: (Float, Float) -> Float) {

    /**
     * Evaluate the [point2D] against the equation of this curve.
     * @param point2D the [Point2D] to evaluate.
     * @return the evaluated result. If the result is 0, the point is considered belong to this curve.
     * @see Curve2D
     */
    fun eval(point2D: Point2D) = func(point2D.x, point2D.y)
}
