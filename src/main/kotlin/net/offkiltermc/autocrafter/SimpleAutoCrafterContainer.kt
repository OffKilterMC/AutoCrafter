package net.offkiltermc.autocrafter

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

class SimpleAutoCrafterContainer(private val level: Level, private val stackSizeLimit: Int?): SimpleContainer(11), AutoCrafterContainer {

    override fun craftingContainer(): ProxiedCraftingContainer {
        return ProxiedCraftingContainer(this, 2, stackSizeLimit)
    }

    override fun canPickup(slotNo: Int): Boolean {
        return true
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