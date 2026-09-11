package com.example.legoigo

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver

class GameCore : ApplicationAdapter() {

    // 3D окружение и камера
    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment

    // Физика
    private lateinit var dynamicsWorld: btDiscreteDynamicsWorld
    private lateinit var solver: btSequentialImpulseConstraintSolver
    private lateinit var dispatcher: btCollisionDispatcher
    private lateinit var broadphase: btDbvtBroadphase
    private lateinit var collisionConfig: btDefaultCollisionConfiguration

    // Игровые компоненты
    private lateinit var gameWorld: GameWorld
    private lateinit var inputHandler: InputHandler
    private lateinit var uiOverlay: PartSelectionOverlay

    override fun create() {
        // Инициализация физического движка Bullet
        Bullet.init()

        // Настройка камеры
        camera = PerspectiveCamera(
            67f,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        ).apply {
            position.set(0f, 5f, 10f)
            lookAt(0f, 0f, 0f)
            near = 1f
            far = 300f
            update()
        }

        // Настройка рендеринга
        modelBatch = ModelBatch()
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f))
            add(DirectionalLight().set(1f, 1f, 1f, -0.5f, -1f, -0.5f))
        }

        // Настройка физического мира
        collisionConfig = btDefaultCollisionConfiguration()
        dispatcher = btCollisionDispatcher(collisionConfig)
        broadphase = btDbvtBroadphase()
        solver = btSequentialImpulseConstraintSolver()
        dynamicsWorld = btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig)

        // ГРАВИТАЦИЯ
        dynamicsWorld.setGravity(Vector3(0f, -9.8f, 0f))

        // Игровой мир
        gameWorld = GameWorld(dynamicsWorld, modelBatch)
        gameWorld.createGround()

        // UI (создаём до мультиплексора, чтобы получить stage)
        uiOverlay = PartSelectionOverlay(gameWorld)

        // Обработчик ввода
        inputHandler = InputHandler(camera, gameWorld) { selectedObject ->
            Gdx.app.log("Game", "Selected: ${selectedObject.partType.displayName}")
        }

        // Мультиплексор: сначала UI, потом игровой ввод
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(uiOverlay.stage)
        multiplexer.addProcessor(inputHandler)
        Gdx.input.inputProcessor = multiplexer
    }

    override fun render() {
        // Очистка экрана — голубое небо
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Физика
        dynamicsWorld.stepSimulation(Gdx.graphics.deltaTime, 5, 1f / 60f)

        // Обновление игрового мира
        gameWorld.update(Gdx.graphics.deltaTime)

        // Рендер 3D
        modelBatch.begin(camera)
        gameWorld.render(modelBatch, environment)
        modelBatch.end()

        // Рендер UI
        uiOverlay.render(Gdx.graphics.deltaTime)
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        if (::uiOverlay.isInitialized) {
            uiOverlay.resize(width, height)
        }
    }

    override fun dispose() {
        if (::uiOverlay.isInitialized) uiOverlay.dispose()
        if (::gameWorld.isInitialized) gameWorld.dispose()
        if (::modelBatch.isInitialized) modelBatch.dispose()
        if (::dynamicsWorld.isInitialized) dynamicsWorld.dispose()
        if (::solver.isInitialized) solver.dispose()
        if (::dispatcher.isInitialized) dispatcher.dispose()
        if (::broadphase.isInitialized) broadphase.dispose()
        if (::collisionConfig.isInitialized) collisionConfig.dispose()
    }
}