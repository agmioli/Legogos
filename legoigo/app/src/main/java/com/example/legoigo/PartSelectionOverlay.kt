package com.example.legoigo

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport

class PartSelectionOverlay(
    private val gameWorld: GameWorld
) {
    val stage: Stage = Stage(ScreenViewport())

    private val skin: Skin = createDefaultSkin()
    private val menuTable: Table
    private val menuButton: TextButton
    private var isMenuOpen = false

    init {
        // Кнопка открытия меню (правый верхний угол)
        menuButton = TextButton("Menu", skin)
        menuButton.setSize(140f, 70f)
        menuButton.setPosition(
            Gdx.graphics.width - 150f,
            Gdx.graphics.height - 80f
        )
        menuButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                toggleMenu()
            }
        })
        stage.addActor(menuButton)

        // Таблица меню со списком деталей
        menuTable = Table()
        menuTable.setFillParent(false)
        menuTable.setPosition(
            Gdx.graphics.width - 260f,
            Gdx.graphics.height - 400f
        )
        menuTable.isVisible = false

        val title = Label("3D details:", skin)
        menuTable.add(title).padBottom(12f).row()

        PartType.entries.forEach { partType ->
            val button = TextButton(partType.displayName, skin)
            button.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    gameWorld.addPart(partType)
                    closeMenu()
                }
            })
            menuTable.add(button).width(240f).height(70f).padBottom(6f).row()
        }

        stage.addActor(menuTable)
    }

    private fun toggleMenu() {
        isMenuOpen = !isMenuOpen
        menuTable.isVisible = isMenuOpen
        menuButton.setText(if (isMenuOpen) "Close" else "Menu")
    }

    private fun closeMenu() {
        isMenuOpen = false
        menuTable.isVisible = false
        menuButton.setText("Menu")
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        menuButton.setPosition(width - 150f, height - 80f)
        menuTable.setPosition(width - 260f, height - 400f)
    }

    fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    fun dispose() {
        stage.dispose()
        skin.dispose()
    }

    private fun createDefaultSkin(): Skin {
        val skin = Skin()
        val font = BitmapFont()
        skin.add("default-font", font)

        // LabelStyle
        skin.add("default", Label.LabelStyle(font, Color.WHITE))

        // Простые цветные drawable для кнопок
        val upTex = solidTexture(Color(0.25f, 0.25f, 0.25f, 0.9f))
        val downTex = solidTexture(Color(0.45f, 0.45f, 0.45f, 1f))
        val overTex = solidTexture(Color(0.35f, 0.35f, 0.35f, 1f))

        val style = TextButton.TextButtonStyle()
        style.font = font
        style.fontColor = Color.WHITE
        style.up = TextureRegionDrawable(TextureRegion(upTex))
        style.down = TextureRegionDrawable(TextureRegion(downTex))
        style.over = TextureRegionDrawable(TextureRegion(overTex))
        skin.add("default", style)

        return skin
    }

    private fun solidTexture(color: Color): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }
}