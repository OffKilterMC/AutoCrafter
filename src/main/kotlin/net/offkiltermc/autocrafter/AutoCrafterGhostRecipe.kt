package net.offkiltermc.autocrafter

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.FastColor
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeHolder

class AutoCrafterGhostRecipe(
    recipe: RecipeHolder<*>,
    private val startIndex: Int,
    private val gridWidth: Int,
    private val gridHeight: Int
) {
    val ingredients = HashMap<Int, Ingredient>()

    init {
        val placer = AutoCrafterRecipePlacer()
        placer.placeRecipe(
            gridWidth,
            gridHeight,
            startIndex,
            recipe,
            recipe.value().ingredients.iterator(),
        ) { iterator, slotNo ->
            val ingredient: Ingredient = iterator.next()
            if (!ingredient.isEmpty) {
                ingredients[slotNo] = ingredient
            }
        }
    }

    fun render(guiGraphics: GuiGraphics, slots: List<Slot>, minecraft: Minecraft, i: Int, j: Int) {
        for (slotNo in startIndex until startIndex + 9) {
            val slot = slots[slotNo]
            val l = slot.x + i
            val m = slot.y + j

            val ingredient = ingredients[slotNo]
            if (ingredient != null) {
                val itemStack = ingredient.items[0]

                if (!slot.item.`is`(itemStack.item)) {
                    // slot is empty, just draw the desired ingredient ghost
                    if (slot.item == ItemStack.EMPTY) {
                        guiGraphics.renderFakeItem(itemStack, l, m)
                        val color = FastColor.ARGB32.color(80, 255, 255, 255)
                        guiGraphics.fill(RenderType.guiGhostRecipeOverlay(), l, m, l + 16, m + 16, color)
                        guiGraphics.renderItemDecorations(minecraft.font, itemStack, l, m)
                    } else {
                        // item doesn't match ingredient, signal error
                        val color = FastColor.ARGB32.color(30, 255, 0, 0)
                        guiGraphics.fill(l, m, l + 16, m + 16, color)
                    }
                }
            } else if (slot.item != ItemStack.EMPTY) {
                // there's no ingredient in this spot, but there is an item
                guiGraphics.fill(l, m, l + 16, m + 16, 0x30FF0000)
            }
        }
    }

}