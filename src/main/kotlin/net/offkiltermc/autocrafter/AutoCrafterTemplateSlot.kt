package net.offkiltermc.autocrafter

import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot

class AutoCrafterTemplateSlot(
    container: Container,
    i: Int, j: Int, k: Int
) : Slot(container, i, j, k) {

    // We just want to limit the size to 1
    override fun getMaxStackSize(): Int {
        return 1
    }

}