package net.offkiltermc.autocrafter

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.ResultSlot

class AutoCrafterResultSlot(
    player: Player,
    craftingContainer: CraftingContainer,
    container: Container,
    i: Int,
    j: Int,
    k: Int
) : ResultSlot(player, craftingContainer, container, i, j, k) {

    override fun mayPickup(player: Player): Boolean {
        // Can't take result if there is a template item
        return if (container.getItem(1).isEmpty) {
            super.mayPickup(player)
        } else {
            false
        }
    }

}