package com.example.gpssatelliteviewer.scene3d.manager.camera

import android.util.Log
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.normalized
import com.google.android.filament.utils.Manipulator
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.transform
import io.github.sceneview.math.Transform
import kotlin.math.abs

private const val EPS = 1e-3f

class ClampedCameraManipulator(
    manipulator: Manipulator,
    private val minCameraDistance: Float = 0.6f,
    private val maxPitchDeg: Float = 85f
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

        // continuously enforce clamps in update, in case other code changes camera
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
            val scale = prevSeparation / currSeparation
            val transform = super.getTransform()
            val camPos = transform.position
            val camDist = camPos.length()

            if (camDist >= minCameraDistance || scale > 1f) {
                super.scrollUpdate(x, y, prevSeparation, currSeparation)
                t = clampTransform(super.getTransform())
                //Log.d(TAG, "scroll allowed transform=$transform\nclamped t=$t\ncamDist=$camDist")
            } else {
                val camDir = if (camDist > 0f) camPos.normalized() else Float3(0f, 0f, -1f)
                val camClampedDist = camDir * minCameraDistance

                val tTransform = transform
                tTransform.position = camClampedDist
                t = clampTransform(tTransform)

                super.scrollUpdate(x, y, 0.0f, 0.0f)
                //Log.d(TAG, "Blocked pinch; set tTransform=$tTransform camDist=$camDist")
            }
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
        Log.d(TAG, "grabEnd")
        super.grabEnd()

        t = clampTransform(super.getTransform())
    }

    private fun clampTransform(transform: Transform): Transform {
        val pos = transform.position
        val dist = pos.length()

        if (dist > 0f && dist < minCameraDistance) {
            val dir = pos.normalized()
            val newPos = dir * minCameraDistance
            //Log.d(TAG, "Clamping distance: $dist -> ${newPos.length()}")
            transform.position = newPos
        }

        return transform
    }
}
