package net.offkiltermc.autocrafter

import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.ShapedRecipe

class AutoCrafterRecipePlacer {
    data class Size(val width: Int, val height: Int)

    private fun determineRecipeBounds(defaultSize: Size, recipeHolder: RecipeHolder<*>): Size {
        val recipe = recipeHolder.value()
        return if (recipe is ShapedRecipe) {
            Size(recipe.width, recipe.height)
        } else {
            defaultSize
        }
    }

    fun placeRecipe(
        gridWidth: Int,
        gridHeight: Int,
        startingSlotNumber: Int,
        recipe: RecipeHolder<*>,
        iterator: Iterator<Ingredient>,
        closure: (iterator: Iterator<Ingredient>, i: Int) -> Unit
    ) {
        val recipeSize = determineRecipeBounds(Size(gridWidth, gridHeight), recipe)

        for (row in 0 until recipeSize.height) {
            for (col in 0 until recipeSize.width) {
                if (!iterator.hasNext()) {
                    return
                }

                val slotNo = startingSlotNumber + (row * gridWidth) + col
                closure(iterator, slotNo)
            }
        }
    }
}
