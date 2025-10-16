package com.example.gpssatelliteviewer.utils

import dev.romainguy.kotlin.math.Float3
import kotlin.math.sqrt

fun Iterable<Float>.averageOrNull(): Double? = if (this.any()) this.average() else null

fun Float3.length(): Float = sqrt(x * x + y * y + z * z)

fun Float3.normalized(): Float3 {
    val len = length()
    return Float3(x / len, y / len, z / len)
}

fun Float3.normalized(length: Float): Float3 {
    return Float3(x / length, y / length, z / length)
}
