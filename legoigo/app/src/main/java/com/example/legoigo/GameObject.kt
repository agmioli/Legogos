package com.example.legoigo

import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState

enum class PartType(
    val displayName: String,
    val diffuse: ColorAttribute,
    val shapeType: ShapeType
) {
    RED_CUBE("Red cube", ColorAttribute.createDiffuse(1f, 0f, 0f, 1f), ShapeType.BOX),
    YELLOW_BOX("Yellow box", ColorAttribute.createDiffuse(1f, 1f, 0f, 1f), ShapeType.BOX),
    BLACK_WHEEL("Black wheel", ColorAttribute.createDiffuse(0.1f, 0.1f, 0.1f, 1f), ShapeType.CYLINDER),
    GREEN_TRIANGLE("Green triangle", ColorAttribute.createDiffuse(0f, 1f, 0f, 1f), ShapeType.BOX)
}

enum class ShapeType { BOX, CYLINDER }

class GameObject(
    val partType: PartType,
    spawnPosition: Vector3
) {
    val modelInstance: ModelInstance
    val rigidBody: btRigidBody
    private val motionState: btDefaultMotionState
    private val shape: btCollisionShape

    var isDragging = false
    private val dragOffset = Vector3()

    private val tmpVec = Vector3()
    private val tmpMat = Matrix4()

    init {
        val model: Model = createModel(partType)
        modelInstance = ModelInstance(model)
        modelInstance.transform.setToTranslation(spawnPosition)

        val pair = createPhysicsShape(partType)
        shape = pair.first
        val mass: Float = pair.second

        motionState = btDefaultMotionState(
            Matrix4().setToTranslation(spawnPosition)
        )

        val inertia = Vector3()
        shape.calculateLocalInertia(mass, inertia)

        val bodyInfo = btRigidBody.btRigidBodyConstructionInfo(
            mass,
            motionState,
            shape,
            inertia
        )

        rigidBody = btRigidBody(bodyInfo)

        rigidBody.friction = 0.5f
        rigidBody.restitution = 0.3f

        bodyInfo.dispose()
    }

    private fun createModel(type: PartType): Model {
        val builder = ModelBuilder()
        val material = Material(type.diffuse)
        val usage: Long = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

        return when (type) {
            PartType.RED_CUBE -> builder.createBox(1f, 1f, 1f, material, usage)
            PartType.YELLOW_BOX -> builder.createBox(2f, 1f, 1f, material, usage)
            PartType.BLACK_WHEEL -> builder.createCylinder(1.5f, 0.5f, 1.5f, 16, material, usage)
            PartType.GREEN_TRIANGLE -> builder.createBox(1.5f, 1.5f, 0.5f, material, usage)
        }
    }

    private fun createPhysicsShape(type: PartType): Pair<btCollisionShape, Float> {
        return when (type.shapeType) {
            ShapeType.BOX -> {
                val half: Vector3 = when (type) {
                    PartType.RED_CUBE -> Vector3(0.5f, 0.5f, 0.5f)
                    PartType.YELLOW_BOX -> Vector3(1f, 0.5f, 0.5f)
                    PartType.GREEN_TRIANGLE -> Vector3(0.75f, 0.75f, 0.25f)
                    else -> Vector3(0.5f, 0.5f, 0.5f)
                }
                btBoxShape(half) to 1f
            }
            ShapeType.CYLINDER -> btCylinderShape(Vector3(0.75f, 0.75f, 0.25f)) to 1f
        }
    }

    fun updateTransform() {
        if (!isDragging) {
            motionState.getWorldTransform(modelInstance.transform)
        }
    }

    fun startDrag(worldPosition: Vector3) {
        isDragging = true

        // ★★ ГЛАВНОЕ ИЗМЕНЕНИЕ ★★
        // Ставим флаг "нет отклика на столкновения" — во время перетаскивания
        // деталь пролетает сквозь другие тела, не отталкивая их и не отталкиваясь сама.
        setNoContactResponse(true)

        rigidBody.activate()
        rigidBody.setGravity(Vector3.Zero)

        modelInstance.transform.getTranslation(tmpVec)
        dragOffset.set(worldPosition).sub(tmpVec)
    }

    fun updateDrag(worldPosition: Vector3) {
        if (!isDragging) return
        val newPos = tmpVec.set(worldPosition).sub(dragOffset)
        modelInstance.transform.setTranslation(newPos)
        tmpMat.set(modelInstance.transform)
        motionState.setWorldTransform(tmpMat)
        rigidBody.setWorldTransform(tmpMat)
    }

    fun endDrag() {
        isDragging = false

        // Снимаем флаг — теперь деталь снова взаимодействует с другими телами.
        // Гравитация "посадит" её на верхнюю поверхность того, что под ней.
        setNoContactResponse(false)

        rigidBody.setGravity(Vector3(0f, -9.8f, 0f))
        tmpMat.set(modelInstance.transform)
        motionState.setWorldTransform(tmpMat)
        rigidBody.setWorldTransform(tmpMat)
        rigidBody.activate()
    }

    fun rotate(deltaX: Float, deltaY: Float) {
        if (!isDragging) return
        modelInstance.transform.rotate(Vector3.Y, deltaX * 0.5f)
        modelInstance.transform.rotate(Vector3.X, deltaY * 0.5f)
        tmpMat.set(modelInstance.transform)
        motionState.setWorldTransform(tmpMat)
        rigidBody.setWorldTransform(tmpMat)
    }

    /**
     * Управляет флагом CF_NO_CONTACT_RESPONSE у физического тела.
     * Когда флаг установлен — тело не получает отклика от столкновений.
     */
    private fun setNoContactResponse(enabled: Boolean) {
        val flag = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        val current = rigidBody.collisionFlags
        rigidBody.collisionFlags = if (enabled) {
            current or flag
        } else {
            current and flag.inv()
        }
    }

    fun dispose() {
        rigidBody.dispose()
        motionState.dispose()
        shape.dispose()
    }
}