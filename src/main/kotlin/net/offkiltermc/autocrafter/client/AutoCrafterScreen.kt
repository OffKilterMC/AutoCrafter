package net.offkiltermc.autocrafter.client

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.offkiltermc.autocrafter.AutoCrafterGhostRecipe
import net.offkiltermc.autocrafter.AutoCrafterMenu

class AutoCrafterScreen(craftingMenu: AutoCrafterMenu, inventory: Inventory, component: Component) :
    AbstractContainerScreen<AutoCrafterMenu>(craftingMenu, inventory, component) {
    private var allowChangingTemplate = false

    private enum class Button {
        UP,
        DOWN
    }

    private data class Point(
        val x: Int, val y: Int
    )

    private data class Rect(
        val x: Int, val y: Int, val width: Int, val height: Int
    ) {
        val maxX = x + width
        val maxY = y + height

        fun contains(point: Point): Boolean {
            return !(point.x < this.x || point.x > this.maxX || point.y < this.y || point.y > this.maxY)
        }
    }

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.render(guiGraphics, i, j, f)

        menu.recipeInfo()?.recipe()?.let { recipe ->
            val gr = AutoCrafterGhostRecipe(recipe, 2, 3, 3)
            gr.render(guiGraphics, menu.slots, minecraft!!, this.leftPos, this.topPos)
        }

        this.allowChangingTemplate = menu.targetItemCanBeCraftedMultipleWays()
        if (allowChangingTemplate) {
            renderButton(Button.UP, guiGraphics, i, j)
            renderButton(Button.DOWN, guiGraphics, i, j)
        }

        renderTooltip(guiGraphics, i, j)
    }

    private fun renderButton(button: Button, guiGraphics: GuiGraphics, i: Int, j: Int) {
        val rect = getButtonRect(button)
        val isHovered = rect.contains(Point(i, j))
        val texPt = getArrowTextureXY(button, isHovered)

        guiGraphics.blit(
            AUTO_CRAFTER_BG_LOCATION,
            rect.x,
            rect.y,
            texPt.x.toFloat(),
            texPt.y.toFloat(),
            ICON_SIZE,
            ICON_SIZE,
            TEXTURE_SIZE,
            TEXTURE_SIZE
        )
    }

    override fun mouseScrolled(x: Double, y: Double, f: Double, vertAmount: Double): Boolean {
        return if (allowChangingTemplate && hoveredSlot?.index == TEMPLATE_SLOT) {
            if (vertAmount < 0) {
                doButtonClick(0)
            } else {
                doButtonClick(1)
            }

            true
        } else {
            super.mouseScrolled(x, y, f, vertAmount)
        }
    }

    override fun mouseClicked(d: Double, e: Double, i: Int): Boolean {
        if (allowChangingTemplate) {
            getButtonHit(d, e)?.let {
                when (it) {
                    Button.UP -> doButtonClick(0)
                    Button.DOWN -> doButtonClick(1)
                }
                return true
            }
        }
        return super.mouseClicked(d, e, i)
    }

    override fun renderBg(guiGraphics: GuiGraphics, f: Float, i: Int, j: Int) {
        val k = leftPos
        val l = (height - imageHeight) / 2
        guiGraphics.blit(AUTO_CRAFTER_BG_LOCATION, k, l, 0, 0, imageWidth, imageHeight)
    }

    private fun doButtonClick(index: Int) {
        menu.clickMenuButton(minecraft!!.player!!, index)
        minecraft!!.gameMode!!.handleInventoryButtonClick(menu.containerId, index)
    }

    private fun getButtonHit(x: Double, y: Double): Button? {
        if (getButtonRect(Button.UP).contains(Point(x.toInt(), y.toInt()))) {
            return Button.UP
        }
        if (getButtonRect(Button.DOWN).contains(Point(x.toInt(), y.toInt()))) {
            return Button.DOWN
        }
        return null
    }

    private fun getArrowTextureXY(button: Button, isHovered: Boolean): Point {
        val x = when (button) {
            Button.UP -> ICON_SIZE
            Button.DOWN -> 0
        }
        val y = this.imageHeight + if (isHovered) { ICON_SIZE } else { 0 }

        return Point(x, y)
    }


    private fun getButtonRect(button: Button): Rect {
        return when (button) {
            Button.UP -> Rect(this.leftPos + 12, this.topPos + 16, ICON_SIZE, ICON_SIZE)
            Button.DOWN -> Rect( this.leftPos + 12, this.topPos + 55, ICON_SIZE, ICON_SIZE)
        }
    }

    companion object {
        private val AUTO_CRAFTER_BG_LOCATION = ResourceLocation("autocrafter:textures/gui/auto_crafting_table.png")
        private const val ICON_SIZE = 16
        private const val TEXTURE_SIZE = 256
        private const val TEMPLATE_SLOT = 1
    }
}