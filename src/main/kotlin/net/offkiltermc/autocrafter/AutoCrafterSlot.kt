package net.offkiltermc.autocrafter

import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class AutoCrafterSlot(container: AutoCrafterContainer, private val slotNo: Int, x: Int, y: Int): Slot(container, slotNo, x, y) {
//    override fun mayPlace(itemStack: ItemStack): Boolean {
//        return (container as AutoCrafterContainer).canPlaceItem(slotNo, itemStack)
//    }
//
//    override fun mayPickup(player: Player): Boolean {
//        return (container as AutoCrafterContainer).canPickup(slotNo)
//    }

    override fun onTake(player: Player, itemStack: ItemStack) {
        // do nothing
    }
}