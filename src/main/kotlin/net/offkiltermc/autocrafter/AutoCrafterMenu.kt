package net.offkiltermc.autocrafter

import com.mojang.logging.LogUtils
import net.minecraft.client.gui.screens.recipebook.RecipeCollection
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.level.Level
import net.offkiltermc.autocrafter.AutoCrafter.Companion.AUTO_CRAFTER_MENU

class AutoCrafterMenu(
    i: Int,
    inventory: Inventory,
    private val container: AutoCrafterContainer,
    private val access: ContainerLevelAccess,
    private val dataAccess: ContainerData
) :
    AbstractContainerMenu(AUTO_CRAFTER_MENU, i) {
    private val player: Player = inventory.player
    private val recipeFinder = CachedRecipeFinder(player.level())

    constructor(i: Int, inventory: Inventory) : this(
        i,
        inventory,
        SimpleAutoCrafterContainer(inventory.player.level()),
        ContainerLevelAccess.NULL,
        SimpleContainerData(1)
    )

    init {
        // Result slot
        addSlot(
            AutoCrafterResultSlot(
                inventory.player,
                container.craftingContainer(),
                container,
                RESULT_SLOT,
                RESULT_SLOT_X,
                RESULT_SLOT_Y
            )
        )

        // Template slot
        addSlot(
            AutoCrafterTemplateSlot(
                container,
                TEMPLATE_SLOT, TEMPLATE_SLOT_X, TEMPLATE_SLOT_Y
            )
        )

        // Main crafting grid
        for (row in 0..2) {
            for (col in 0..2) {
                addSlot(
                    Slot(
                        container,
                        2 + (col + row * 3),
                        GRID_X + col * GRID_SPACING,
                        GRID_Y + row * GRID_SPACING
                    )
                )
            }
        }

        // Main inventory (slots 9-26)
        for (row in 0..2) {
            for (col in 0 until ITEMS_PER_ROW) {
                addSlot(
                    Slot(
                        inventory,
                        col + row * ITEMS_PER_ROW + ITEMS_PER_ROW,
                        INVENTORY_X + col * GRID_SPACING,
                        INVENTORY_Y + row * GRID_SPACING
                    )
                )
            }
        }

        // Hot bar (slots 0-8)
        for (slot in 0 until ITEMS_PER_ROW) {
            addSlot(Slot(inventory, slot, HOT_BAR_X + slot * GRID_SPACING, HOT_BAR_Y))
        }

        addDataSlots(dataAccess)
    }

    override fun setItem(i: Int, j: Int, itemStack: ItemStack) {
        super.setItem(i, j, itemStack)
        if (i == TEMPLATE_SLOT) {
            setData(0, 0)
        }
    }

    // this is just to add one to the second index as I think it's odd to have to add 1
    // when I know the explicit first and last index to move to.
    override fun moveItemStackTo(itemStack: ItemStack, i: Int, j: Int, bl: Boolean): Boolean {
        return super.moveItemStackTo(itemStack, i, j + 1, bl)
    }

    override fun quickMoveStack(player: Player, i: Int): ItemStack {
        //LOGGER.info("QUICKMOVE START")
        var itemStack = ItemStack.EMPTY
        val slot = slots[i]
        if (slot.hasItem()) {
            val itemStack2 = slot.item
            itemStack = itemStack2.copy()
            if (i == RESULT_SLOT) {
                this.access.execute { level: Level, _: BlockPos ->
                    itemStack2.item.onCraftedBy(itemStack2, level, player)
                }
                if (!moveItemStackTo(itemStack2, FIRST_INVENTORY_SLOT, LAST_INVENTORY_SLOT, true)) {
                    //LOGGER.info("QUICKMOVE RETURNING EMPTY 1")
                    return ItemStack.EMPTY
                }
                slot.onQuickCraft(itemStack2, itemStack)
            } else if (i in FIRST_CRAFTING_SLOT..LAST_CRAFTING_SLOT) {
                if (!moveItemStackTo(itemStack2, FIRST_INVENTORY_SLOT, LAST_INVENTORY_SLOT, false)) {
                    //LOGGER.info("QUICKMOVE RETURNING EMPTY 2")
                    return ItemStack.EMPTY
                }
            } else if (i in FIRST_INVENTORY_SLOT..LAST_INVENTORY_SLOT) {
                // try crafting grid
                var moved = moveItemStackTo(itemStack2, FIRST_CRAFTING_SLOT, LAST_CRAFTING_SLOT, false)
                if (!moved) {
                    moved = if (i < FIRST_HOTBAR_SLOT) {
                        moveItemStackTo(itemStack2, FIRST_HOTBAR_SLOT, LAST_INVENTORY_SLOT, false)
                    } else {
                        moveItemStackTo(itemStack2, FIRST_INVENTORY_SLOT, FIRST_HOTBAR_SLOT - 1, false)
                    }
                }
                if (!moved) {
                    //LOGGER.info("QUICKMOVE RETURNING EMPTY 3")
                    return ItemStack.EMPTY
                }
            }
            slot.onTake(player, itemStack2)
            if (itemStack2.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
            if (itemStack2.count == itemStack.count) {
                //LOGGER.info("QUICKMOVE RETURNING EMPTY 4")
                return ItemStack.EMPTY
            }
            if (i == RESULT_SLOT) {
                player.drop(itemStack2, false)
            }
        }

        //LOGGER.info("QUICKMOVE RETURNING $itemStack")
        return itemStack
    }

    override fun stillValid(player: Player): Boolean {
        return container.stillValid(player)
    }

    override fun canTakeItemForPickAll(itemStack: ItemStack, slot: Slot): Boolean {
        return slot.index != RESULT_SLOT && super.canTakeItemForPickAll(itemStack, slot)
    }

    fun recipeInfo(): RecipeInfo? {
        val templateItem = getSlot(TEMPLATE_SLOT).item
        if (templateItem.isEmpty) {
            return null
        }

        return RecipeInfo(
            RecipeCollection(player.level().registryAccess(), recipeFinder.allRecipesForItem(templateItem)),
            dataAccess.get(0)
        )
    }

    override fun clickMenuButton(player: Player, i: Int): Boolean {
        val recipes = recipeFinder.allRecipesForItem(items[TEMPLATE_SLOT])
        return when (i) {
            0 -> {
                val newValue = (dataAccess.get(0) + 1).mod(recipes.size)
                setData(0, newValue)
                true
            }

            1 -> {
                val backOne = dataAccess.get(0) - 1
                val newValue = if (backOne < 0) { recipes.size - 1 } else { backOne }
                setData(0, newValue)
                true
            }

            else -> {
                false
            }
        }
    }

    fun targetItemCanBeCraftedMultipleWays(): Boolean {
        return (recipeInfo()?.collection?.recipes?.count() ?: 0) > 1
    }

    data class RecipeInfo(val collection: RecipeCollection, val index: Int) {
        fun recipe(): RecipeHolder<*>? {
            if (index >= 0) {
                return collection.recipes[index]
            }
            return null
        }
    }

    private companion object {
        const val ITEMS_PER_ROW = 9
        const val GRID_SPACING = 18
        const val INVENTORY_Y = 84
        const val INVENTORY_X = 8
        const val HOT_BAR_X = 8
        const val HOT_BAR_Y = 142

        const val TEMPLATE_SLOT_X = 12
        const val TEMPLATE_SLOT_Y = 35

        const val RESULT_SLOT_X = 147
        const val RESULT_SLOT_Y = 35

        const val GRID_X = 62
        const val GRID_Y = 17

        const val RESULT_SLOT = 0
        const val TEMPLATE_SLOT = 1
        const val FIRST_INVENTORY_SLOT = 11
        const val FIRST_HOTBAR_SLOT = 38
        const val LAST_INVENTORY_SLOT = 46
        const val FIRST_CRAFTING_SLOT = 2
        const val LAST_CRAFTING_SLOT = 10

        private val LOGGER = LogUtils.getLogger()
    }

}