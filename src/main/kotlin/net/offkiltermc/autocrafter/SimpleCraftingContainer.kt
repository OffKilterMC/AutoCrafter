package net.offkiltermc.autocrafter

import net.minecraft.world.SimpleContainer
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack

class SimpleCraftingContainer(private val width: Int, private val height: Int): SimpleContainer(width * height), CraftingContainer {
    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun getItems(): MutableList<ItemStack> {
        return items.toMutableList()
    }

}