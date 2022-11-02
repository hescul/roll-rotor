package com.flyng.rotor.foundation.geometry

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class Curve2DTest {
    private val curve = Curve2D { x, y ->
        sqrt(x) + sqrt(y) - 1
    }

    @Test
    fun pointOnCurve() {
        val point = Point2D(0, 1)
        assertTrue("point (0, 1) must be on the curve x^2 + y^2 - 1 = 0") { curve.eval(point) == 0.0f }
    }

    @Test
    fun pointInsideCurve() {
        val point = Point2D(0, 0)
        assertTrue("point (0, 1) must be inside the curve x^2 + y^2 - 1 = 0") { curve.eval(point) < 0.0f }
    }

    @Test
    fun pointOutsideCurve() {
        val point = Point2D(1, 1)
        assertTrue("point (1, 1) must be outside the curve x^2 + y^2 - 1 = 0") { curve.eval(point) > 0.0f }
    }
}