package com.example.gpssatelliteviewer.scene3d.manager.camera

import android.util.Log
import com.example.gpssatelliteviewer.data.EPS
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.normalized
import com.google.android.filament.utils.Manipulator
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.transform
import io.github.sceneview.math.Transform
import kotlin.math.abs

// max distance camera can zoom in
private const val MAX_SEPARATION = 30f  // 1 = ~0.005 scene distance
private const val UNIT_SCALE = 0.005f

class ClampedCameraManipulator(
    manipulator: Manipulator,
    private val minCameraDistance: Float,
    private val maxCameraDistance: Float,
    private val maxPitchDeg: Float,
) : CameraGestureDetector.DefaultCameraManipulator(manipulator) {

    private var mY = 0
    private var t: Transform = manipulator.transform

    private val TAG = "CameraDebug"

    override fun setViewport(width: Int, height: Int) {
        Log.d(TAG, "Went in setViewport")
        super.setViewport(width, height)
    }

    override fun getTransform(): Transform {
        return t
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        try {
            val produced = super.getTransform()
            val clamped = clampTransform(produced)
            t = clamped
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clamp camera in update: ${e.message}")
        }
    }

    override fun scrollBegin(x: Int, y: Int, separation: Float) {
        super.scrollBegin(x, y, separation)
    }

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
        if (prevSeparation > 0f && currSeparation > 0f && abs(currSeparation - prevSeparation) > 1f) {

            val before = super.getTransform()
            val totalDelta = currSeparation - prevSeparation
            val direction = if (totalDelta > 0f) 1f else -1f
            val totalAbs = abs(totalDelta)

            var lastSeparation = prevSeparation
            var lastTransform = before

            val fullSteps = (totalAbs / MAX_SEPARATION).toInt()
            val remainder = totalAbs % MAX_SEPARATION

            fun applyStep(nextSeparation: Float): Boolean {
                super.scrollUpdate(x, y, lastSeparation, nextSeparation)

                val produced = super.getTransform()
                val beforeDist = lastTransform.position.length()
                val producedDist = produced.position.length()

                val withinBounds = producedDist >= minCameraDistance - EPS && producedDist <= maxCameraDistance + EPS
                val movedBackTowardAllowed = when {
                    producedDist > maxCameraDistance + EPS -> producedDist + EPS < beforeDist
                    producedDist < minCameraDistance - EPS -> producedDist - EPS > beforeDist
                    else -> false
                }
                //val diff = abs(beforeDist - producedDist)
                //Log.d(TAG, "lastSep=$lastSeparation nextSep=$nextSeparation beforeDist=$beforeDist producedDist=$producedDist diff=${diff}")

                if (withinBounds || movedBackTowardAllowed) {
                    lastTransform = clampTransform(produced)
                    lastSeparation = nextSeparation
                    return true
                } else {
                    // revert last attempted change
                    val distToBound =
                        if (producedDist < minCameraDistance)
                            beforeDist - minCameraDistance
                        else
                            beforeDist - maxCameraDistance
                    val allowedSepDelta = distToBound / UNIT_SCALE
                    val correctedSeparation = lastSeparation + allowedSepDelta + 4 // +4 for edge cases
                    super.scrollUpdate(x, y, lastSeparation, correctedSeparation)
                    val corrected = super.getTransform()
                    lastTransform = clampTransform(corrected)
                    lastSeparation = correctedSeparation

                    Log.d(
                        TAG,
                        "Out of bounds: before=$beforeDist produced=$producedDist " +
                                "targetBound=${if (producedDist < minCameraDistance) minCameraDistance else maxCameraDistance} " +
                                "distToBound=$distToBound allowedSepDelta=$allowedSepDelta"
                    )
                    return false
                }
            }

            for (i in 1..fullSteps) {
                val nextSeparation = lastSeparation + direction * MAX_SEPARATION
                if (!applyStep(nextSeparation)) break
            }

            if (remainder > 0f) {
                val nextSeparation = lastSeparation + direction * remainder
                applyStep(nextSeparation)
            }

            t = lastTransform

            /* lerp sized steps
            val before = super.getTransform()

            val totalDelta = currSeparation - prevSeparation
            val totalAbs = abs(totalDelta)

            // How many interpolation samples to use (minimum 1)
            val steps = (totalAbs / maxSeparationDelta).toInt().coerceAtLeast(1)
            val alphaStep = 1f / steps

            var lastSeparation = prevSeparation
            var lastTransform = before

            //Log.d(TAG, "$steps currSeparation=$currSeparation lastSep=$prevSeparation delta=${totalDelta} alphaStep=${alphaStep}")
            for (i in 1..steps) {
                val alpha = i * alphaStep
                val desiredSeparation = lerp(prevSeparation, currSeparation, alpha)

                // compute delta from the *last* applied separation (moving window)
                val delta = desiredSeparation - lastSeparation
                val clampedDelta = delta.coerceIn(-maxSeparationDelta, maxSeparationDelta)

                // next separation to apply this step
                val nextSeparation = lastSeparation + clampedDelta

                // IMPORTANT: use lastSeparation as start and nextSeparation as end
                super.scrollUpdate(x, y, lastSeparation, nextSeparation)

                val produced = super.getTransform()

                val beforeDist = lastTransform.position.length()
                val producedDist = produced.position.length()

                val withinBounds = producedDist >= minCameraDistance - EPS && producedDist <= maxCameraDistance + EPS
                val movedBackTowardAllowed = when {
                    producedDist > maxCameraDistance + EPS -> producedDist + EPS < beforeDist
                    producedDist < minCameraDistance - EPS -> producedDist - EPS > beforeDist
                    else -> false
                }

                //Log.d(TAG, "smoothStep=$i/$steps desired=$desiredSeparation lastSep=$lastSeparation nextSep=$nextSeparation beforeDist=$beforeDist producedDist=$producedDist")

                if (withinBounds || movedBackTowardAllowed) {
                    lastTransform = clampTransform(produced)
                    lastSeparation = nextSeparation
                } else {
                    // revert the last attempted change and stop
                    super.scrollUpdate(x, y, nextSeparation, lastSeparation)
                    break
                }
            }

            t = clampTransform(lastTransform)

             */
        }
    }

    override fun scrollEnd() {
        super.scrollEnd()
        t = clampTransform(super.getTransform())
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        //Log.d(TAG, "grabBegin, strafe=${strafe}")

        // prevents camera from panning
        // ??why this??
        if (!strafe) {
            super.grabBegin(x, y, strafe)
            mY = y
            t = super.getTransform()
        }
    }

    override fun grabUpdate(x: Int, y: Int) {
        val before = super.getTransform()

        super.grabUpdate(x, y)

        val produced = super.getTransform()

        val beforePitch = before.rotation.x
        val producedPitch = produced.rotation.x
        val absBefore = abs(beforePitch)
        val absProduced = abs(producedPitch)

        val withinLimit = absProduced <= maxPitchDeg + EPS
        val movedBackTowardZero = absProduced + EPS < absBefore

        //Log.d(TAG, "grabUpdate: mY=$mY y=$y beforePitch=$beforePitch producedPitch=$producedPitch")

        if (withinLimit || movedBackTowardZero) {
            t = produced
            mY = y
            //Log.d(TAG, "Accepted grabUpdate: pitch=$producedPitch (withinLimit=$withinLimit movedBack=$movedBackTowardZero)")
        } else {
            t = before
            super.grabUpdate(x, mY)
            //Log.d(TAG, "Blocked pitch - reverting. beforePitch=$beforePitch producedPitch=$producedPitch")
        }
    }

    override fun grabEnd() {
        //Log.d(TAG, "grabEnd")
        super.grabEnd()

        t = clampTransform(super.getTransform())
    }

    private fun clampTransform(transform: Transform): Transform {
        val pos = transform.position
        val dist = pos.length()

        if (dist <= 0f) return transform

        if (dist < minCameraDistance) {
            val dir = pos.normalized()
            val newPos = dir * minCameraDistance
            //Log.d(TAG, "Clamping distance: $dist -> ${newPos.length()}")
            transform.position = newPos
            return transform
        }

        if (dist > maxCameraDistance) {
            val dir = pos.normalized()
            val newPos = dir * maxCameraDistance
            //Log.d(TAG, "Clamping distance: $dist -> ${newPos.length()}")
            transform.position = newPos
            return transform
        }

        return transform
    }
}
