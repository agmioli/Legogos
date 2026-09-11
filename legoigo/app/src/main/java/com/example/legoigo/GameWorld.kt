package com.example.legoigo

import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState

class GameWorld(
    private val dynamicsWorld: btDiscreteDynamicsWorld,
    private val modelBatch: ModelBatch
) {
    val gameObjects = mutableListOf<GameObject>()

    private lateinit var groundInstance: ModelInstance
    private var groundBody: btRigidBody? = null
    private var groundShape: btBoxShape? = null
    private var groundMotionState: btDefaultMotionState? = null

    private var objectCounter = 0

    fun createGround() {
        val builder = ModelBuilder()
        // Явное приведение к Long, как и в GameObject
        val usage: Long = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        val material = Material(ColorAttribute.createDiffuse(0.2f, 0.7f, 0.2f, 1f))

        val groundModel = builder.createBox(200f, 1f, 200f, material, usage)
        groundInstance = ModelInstance(groundModel, 0f, -0.5f, 0f)

        val shape = btBoxShape(Vector3(100f, 0.5f, 100f))
        val ms = btDefaultMotionState(Matrix4().setToTranslation(0f, -0.5f, 0f))
        val info = btRigidBody.btRigidBodyConstructionInfo(0f, ms, shape, Vector3.Zero)
        val body = btRigidBody(info)

        dynamicsWorld.addRigidBody(body)

        groundBody = body
        groundShape = shape
        groundMotionState = ms

        info.dispose()
    }

    fun addPart(type: PartType) {
        val i = objectCounter++
        val x = (i % 5) * 2f - 4f
        val z = (i / 5) * 2f
        val spawn = Vector3(x, 5f, z)

        val obj = GameObject(type, spawn)
        gameObjects.add(obj)
        dynamicsWorld.addRigidBody(obj.rigidBody)
    }

    fun update(deltaTime: Float) {
        for (obj in gameObjects) obj.updateTransform()
    }

    fun render(batch: ModelBatch, environment: Environment) {
        batch.render(groundInstance, environment)
        for (obj in gameObjects) {
            batch.render(obj.modelInstance, environment)
        }
    }

    fun dispose() {
        for (obj in gameObjects) obj.dispose()
        gameObjects.clear()

        groundBody?.dispose()
        groundShape?.dispose()
        groundMotionState?.dispose()
    }
}