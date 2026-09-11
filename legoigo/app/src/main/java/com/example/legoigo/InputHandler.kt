package com.example.legoigo

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray

class InputHandler(
    private val camera: Camera,
    private val gameWorld: GameWorld,
    private val onObjectSelected: (GameObject) -> Unit
) : InputProcessor {

    private var draggedObject: GameObject? = null

    private val activePointers = mutableMapOf<Int, Vector3>()
    private var pointerCount = 0
    private var lastRotationAngle = 0f

    private fun screenToWorld(screenX: Float, screenY: Float): Vector3 {
        val ray = camera.getPickRay(screenX, screenY)
        val intersection = Vector3()
        val planeY = 0f
        val t = (planeY - ray.origin.y) / ray.direction.y
        if (t > 0) {
            intersection.set(ray.direction).scl(t).add(ray.origin)
        } else {
            intersection.set(ray.direction).scl(10f).add(ray.origin)
        }
        return intersection
    }

    private fun findObjectAt(screenX: Float, screenY: Float): GameObject? {
        val ray: Ray = camera.getPickRay(screenX, screenY)
        var closest: GameObject? = null
        var closestDist = Float.MAX_VALUE

        for (obj in gameWorld.gameObjects) {
            val center = obj.modelInstance.transform.getTranslation(Vector3())
            val radius = getObjectRadius(obj.partType)
            val dist = intersectRaySphere(ray, center, radius)
            if (dist > 0 && dist < closestDist) {
                closestDist = dist
                closest = obj
            }
        }
        return closest
    }

    private fun getObjectRadius(type: PartType): Float = when (type) {
        PartType.RED_CUBE -> 0.8f
        PartType.YELLOW_BOX -> 1.2f
        PartType.BLACK_WHEEL -> 1f
        PartType.GREEN_TRIANGLE -> 1f
    }

    private fun intersectRaySphere(ray: Ray, center: Vector3, radius: Float): Float {
        val dir = Vector3(ray.direction).nor()
        val origin = ray.origin
        val toCenter = Vector3(center).sub(origin)
        val projection = toCenter.dot(dir)
        if (projection < 0) return -1f
        val distSq = toCenter.len2() - projection * projection
        val rSq = radius * radius
        if (distSq > rSq) return -1f
        val halfChord = kotlin.math.sqrt(rSq - distSq)
        return projection - halfChord
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        pointerCount++
        if (pointerCount == 1) {
            val obj = findObjectAt(screenX.toFloat(), screenY.toFloat())
            if (obj != null) {
                draggedObject = obj
                obj.startDrag(screenToWorld(screenX.toFloat(), screenY.toFloat()))
                onObjectSelected(obj)
            }
        } else if (pointerCount == 2) {
            draggedObject?.endDrag()
            lastRotationAngle = 0f
        }
        activePointers[pointer] = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        activePointers[pointer]?.set(screenX.toFloat(), screenY.toFloat(), 0f)

        if (pointerCount == 1 && draggedObject != null) {
            val worldPos = screenToWorld(screenX.toFloat(), screenY.toFloat())
            draggedObject?.updateDrag(worldPos)
        } else if (pointerCount == 2 && draggedObject != null) {
            val pts = activePointers.values.toList()
            if (pts.size >= 2) {
                val p1 = pts[0]
                val p2 = pts[1]
                val angle = kotlin.math.atan2(
                    (p2.y - p1.y).toDouble(),
                    (p2.x - p1.x).toDouble()
                ).toFloat()
                if (lastRotationAngle != 0f) {
                    val delta = angle - lastRotationAngle
                    draggedObject?.rotate(delta * 50f, 0f)
                }
                lastRotationAngle = angle
            }
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        pointerCount = (pointerCount - 1).coerceAtLeast(0)
        activePointers.remove(pointer)
        if (pointerCount == 0) {
            draggedObject?.endDrag()
            draggedObject = null
            lastRotationAngle = 0f
        }
        return true
    }

    // Новый метод, появившийся в LibGDX InputProcessor
    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return touchUp(screenX, screenY, pointer, button)
    }

    override fun keyDown(keycode: Int): Boolean = false
    override fun keyUp(keycode: Int): Boolean = false
    override fun keyTyped(character: Char): Boolean = false
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
    override fun scrolled(amountX: Float, amountY: Float): Boolean = false
}