package net.offkiltermc.autocrafter

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

class CachedRecipeFinder(private val level: Level) {
    private var cachedRecipe: RecipeHolder<*>? = null
    private var allCachedRecipes: List<RecipeHolder<*>> = emptyList()
    private var lastTemplate: ItemStack = ItemStack.EMPTY

    fun allRecipesForItem(itemStack: ItemStack): List<RecipeHolder<*>> {
        if (!itemStack.`is`(lastTemplate.item)) {
            lastTemplate = itemStack.copy()
            cachedRecipe = findRecipeFor(itemStack)
            allCachedRecipes = findAllRecipesFor(itemStack)
        }
        return allCachedRecipes
    }

    private fun findRecipeFor(itemStack: ItemStack): RecipeHolder<*>? {
        if (itemStack.isEmpty) {
            return null
        }
        return level.recipeManager.getAllRecipesFor(RecipeType.CRAFTING).find { item ->
            item.value.getResultItem(level.registryAccess()).`is`(itemStack.item)
        }
    }

    private fun findAllRecipesFor(itemStack: ItemStack): List<RecipeHolder<*>> {
        if (itemStack.isEmpty) {
            return emptyList()
        }
        val result = mutableListOf<RecipeHolder<*>>()
        level.recipeManager.getAllRecipesFor(RecipeType.CRAFTING).forEach { recipe ->
            if (recipe.value.getResultItem(level.registryAccess()).`is`(itemStack.item)) {
                result.add(recipe)
            }
        }
        return result
    }

  }