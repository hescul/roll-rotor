package com.flyng.rotor.foundation.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Point2DTest {
    @Test
    fun coordinateMustBeFloat() {
        val p = Point2D(60L, 0.5f)
        assertIs<Float>(p.x, "x-coordinate is expected to be of type Float")
        assertIs<Float>(p.y, "y-coordinate is expected to be of type Float")
    }

    @Test
    fun coordinateMustNotBeRounded() {
        val (x, y) = Point2D(0.256f, 0.567f)
        assertEquals(x, 0.256f, "x-coordinate value $x is expected to be 0.2")
        assertEquals(y, 0.567f, "y-coordinate value $y is expected to be 0.5")
    }
}