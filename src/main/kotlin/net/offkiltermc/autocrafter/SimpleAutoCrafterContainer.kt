package net.offkiltermc.autocrafter

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

class SimpleAutoCrafterContainer(private val level: Level): SimpleContainer(11), AutoCrafterContainer {

    override fun craftingContainer(): ProxiedCraftingContainer {
        return ProxiedCraftingContainer(this, 2)
    }

    override fun canPickup(slotNo: Int): Boolean {
        return true
    }

    private fun getRecipe(): RecipeHolder<*>? {
        val itemStack = getItem(1)
        if (itemStack.isEmpty) {
            return null
        } else {
            return level.let {
                return it.recipeManager.getAllRecipesFor(RecipeType.CRAFTING).find { item ->
                    item.value.getResultItem(it.registryAccess()).`is`(itemStack.item)
                }
            }
        }
    }

    override fun setItem(i: Int, itemStack: ItemStack) {
        super.setItem(i, itemStack)

        if (i == 1) {
            // set recipe here
        }
    }

    override fun canAddItem(itemStack: ItemStack): Boolean {
        return super.canAddItem(itemStack)
    }

    override fun canPlaceItem(i: Int, itemStack: ItemStack): Boolean {
        return super<SimpleContainer>.canPlaceItem(i, itemStack)
    }

    override fun canTakeItem(container: Container, i: Int, itemStack: ItemStack): Boolean {
        return super<SimpleContainer>.canTakeItem(container, i, itemStack)
    }

}