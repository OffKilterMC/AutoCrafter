package net.offkiltermc.autocrafter

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack

class ProxiedCraftingContainer(private val owner: Container, private val startIndex: Int): CraftingContainer {
    override fun clearContent() {
        for (idx in startIndex until startIndex+9) {
            owner.setItem(idx, ItemStack.EMPTY)
        }
    }

    override fun canPlaceItem(i: Int, itemStack: ItemStack): Boolean {
        return owner.canPlaceItem(i, itemStack)
    }

    override fun canTakeItem(container: Container, i: Int, itemStack: ItemStack): Boolean {
        return owner.canTakeItem(container, i, itemStack)
    }

    override fun getContainerSize(): Int {
        return 9
    }

    override fun isEmpty(): Boolean {
        for (idx in startIndex until startIndex+9) {
            if (!owner.getItem(idx).isEmpty) {
                return false
            }
        }
        return true
    }

    override fun getItem(i: Int): ItemStack {
        return owner.getItem(i + startIndex)
    }

    override fun removeItem(i: Int, j: Int): ItemStack {
        return owner.removeItem(i + startIndex, j)
    }

    override fun removeItemNoUpdate(i: Int): ItemStack {
        return owner.removeItemNoUpdate(i + startIndex)
    }

    override fun setItem(i: Int, itemStack: ItemStack) {
        owner.setItem(i + startIndex, itemStack)
    }

    override fun setChanged() {
        owner.setChanged()
    }

    override fun stillValid(player: Player): Boolean {
        return owner.stillValid(player)
    }

    override fun fillStackedContents(stackedContents: StackedContents) {
        for (itemStack in this.items) {
            stackedContents.accountStack(itemStack)
        }
    }

    override fun getWidth(): Int {
        return 3
    }

    override fun getHeight(): Int {
        return 3
    }

    override fun getItems(): MutableList<ItemStack> {
        val result = mutableListOf<ItemStack>()
        for (i in 0 until containerSize) {
            result.add(getItem(i))
        }
        return result
    }

}