package net.offkiltermc.autocrafter

import net.minecraft.world.Container
import net.minecraft.world.inventory.CraftingContainer

interface AutoCrafterContainer: Container {
    fun craftingContainer(): CraftingContainer

    fun canPickup(slotNo: Int): Boolean

}