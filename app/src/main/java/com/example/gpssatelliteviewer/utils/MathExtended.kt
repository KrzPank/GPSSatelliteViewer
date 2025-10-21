package com.example.gpssatelliteviewer.utils

import dev.romainguy.kotlin.math.Float3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.sin

fun Iterable<Float>.averageOrNull(): Double? = if (this.any()) this.average() else null

fun distance(a: Float3, b: Float3): Float = sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2) + (a.z - b.z).pow(2))

fun lerpF3(a: Float3, b: Float3, alpha: Float): Float3 {
    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return Float3(
        a.x + (b.x - a.x) * clampedAlpha,
        a.y + (b.y - a.y) * clampedAlpha,
        a.z + (b.z - a.z) * clampedAlpha
    )
}

fun lerpF(a: Float, b: Float, alpha: Float): Float {
    val clampedAlpha = alpha.coerceIn(0f, 1f)
    return a + (b - a) * clampedAlpha
}

// Float3 extensions to fit my data types
fun Float3.length(): Float = sqrt(x * x + y * y + z * z)

fun Float3.normalized(): Float3 {
    val len = length()
    return Float3(x / len, y / len, z / len)
}


fun dot(a: Float3, b: Float3): Float = a.x*b.x + a.y*b.y + a.z*b.z

fun cross(a: Float3, b: Float3): Float3 =
    Float3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )


fun add(a: Float3, b: Float3) = Float3(a.x + b.x, a.y + b.y, a.z + b.z)
fun sub(a: Float3, b: Float3) = Float3(a.x - b.x, a.y - b.y, a.z - b.z)
fun scale(v: Float3, s: Float) = Float3(v.x * s, v.y * s, v.z * s)
