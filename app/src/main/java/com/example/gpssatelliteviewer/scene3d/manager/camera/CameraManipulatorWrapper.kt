package com.example.gpssatelliteviewer.scene3d.manager.camera

import android.util.Log
import com.example.gpssatelliteviewer.utils.length
import com.example.gpssatelliteviewer.utils.normalized
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.math.Transform
import io.github.sceneview.node.CameraNode
import kotlin.math.abs

class CameraManipulatorWrapper(
    private val base: CameraGestureDetector.CameraManipulator,
    private val cameraNode: CameraNode,
    private val minCameraDistance: Float = 0.6f,
) : CameraGestureDetector.CameraManipulator {

    override fun setViewport(width: Int, height: Int) {
        Log.d("CameraDebug", "Went in setViewport")
        base.setViewport(width, height)
    }

    override fun getTransform(): Transform {
        return base.getTransform()
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        base.grabBegin(x, y, strafe)
    }

    override fun grabUpdate(x: Int, y: Int) {
        base.grabUpdate(x, y)
    }

    override fun grabEnd() {
        base.grabEnd()
    }

    override fun scrollBegin(x: Int, y: Int, separation: Float) {
        base.scrollBegin(x, y, separation)
    }

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
        Log.d("CameraDebug", "prev=$prevSeparation curr=$currSeparation")

        if (prevSeparation > 0f && currSeparation > 0f && abs(currSeparation - prevSeparation) > 1f) {

            val scale = prevSeparation / currSeparation
            // intended position is always ever so slightly bigger, despite giving magic numbers the difference is getting bigger
            // just leave as it is  error ~0.96
            val intendedPosition = cameraNode.worldPosition * scale
            val intendedDistance = intendedPosition.length()

            //Log.d("CameraDebug", "intendedPosition=$intendedPosition\n  actualPosition=${cameraNode.worldPosition}\n difference=${errorMargin}")
            //Log.d("CameraDebug", "intendedPosition=$intendedPosition actualPosition=${cameraNode.worldPosition}")
            //Log.d("CameraDebug", "intendedDist=$intendedDistance actualDist= ${cameraNode.worldPosition.length()}")

            if (intendedDistance >= minCameraDistance || scale > 1) {
                base.scrollUpdate(x, y, prevSeparation, currSeparation)
            } else {
                val clamped = intendedPosition.normalized() * minCameraDistance

                cameraNode.worldPosition = clamped
                //Log.d("CameraDebug", "Blocked pinch — clamped camera to minDistance")
            }
        }
    }

    override fun scrollEnd() {
        base.scrollEnd()
    }

    override fun update(deltaTime: Float) {
        base.update(deltaTime)
    }
}