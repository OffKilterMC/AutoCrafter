package net.offkiltermc.autocrafter

import com.mojang.logging.LogUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.state.BlockState
import kotlin.jvm.optionals.getOrNull


class AutoCrafterBlockEntity(pos: BlockPos, state: BlockState, private val useDropperBehavior: Boolean) :
    BaseContainerBlockEntity(AutoCrafter.AUTO_CRAFTER_BLOCK_ENTITY!!, pos, state), WorldlyContainer, AutoCrafterContainer {
    // slot 0 is where the result goes
    // slot 1 is where template item lives
    // slots 2-10 is where the recipe part/crafting grid lives
    private var items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY)
    private var lastItemStack = ItemStack.EMPTY
    private var resultWasJustSet = false
    private val recipeFinder by lazy { CachedRecipeFinder(level!!) }
    private var currentRecipeIdentifier: ResourceLocation? = null
    private val dataAccess: ContainerData = object : ContainerData {
        override fun get(i: Int): Int {
            when (i) {
                0 -> {
                    return getRecipeIndex()
                }
            }
            return 0
        }

        override fun set(i: Int, j: Int) {
            when (i) {
                0 -> {
                    setRecipeIndex(j)
                }
            }
        }

        override fun getCount(): Int {
            return 1
        }
    }

    private fun getRecipeIndex(): Int {
        val matches = recipeFinder.allRecipesForItem(items[TEMPLATE_SLOT])
        return matches.indices.find {
            matches[it].id.equals(currentRecipeIdentifier)
        } ?: -1
    }

    private fun setRecipeIndex(idx: Int) {
        if (currentRecipeIdentifier != null) {
            val allMatches = recipeFinder.allRecipesForItem(items[TEMPLATE_SLOT])
            val index = Mth.clamp(idx, 0, allMatches.size - 1)
            currentRecipeIdentifier = allMatches[index].id
            //LOGGER.info("RECIPE INDEX CHANGED new recipe is $currentRecipeIdentifier")
        }
    }

    override fun getSlotsForFace(direction: Direction): IntArray {
        if (direction == Direction.DOWN) {
            return intArrayOf(RESULT_SLOT) + CRAFTING_SLOTS
        }
        return CRAFTING_SLOTS
    }

    override fun canPlaceItem(slot: Int, itemStack: ItemStack): Boolean {
        if (slot == RESULT_SLOT || slot == TEMPLATE_SLOT) {
            return false
        }
        val recipe = getCurrentRecipe() ?: return false
        //LOGGER.info("CANPLACEITEM slot $slot, recipe $recipe")
        val ghost = AutoCrafterGhostRecipe(recipe, FIRST_CRAFTING_SLOT, 3, 3)

        val ingredient = ghost.ingredients[slot] ?: return false
        val itemInSlot = getItem(slot)

        return itemInSlot.isEmpty && ingredient.test(itemStack)
    }

    override fun canPlaceItemThroughFace(slot: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        if (direction == Direction.DOWN) {
            return false
        }
        return canPlaceItem(slot, itemStack)
    }

    override fun canTakeItemThroughFace(slot: Int, itemStack: ItemStack, direction: Direction): Boolean {
        if (direction != Direction.DOWN) {
            return false
        }

        if (useDropperBehavior) {
            return false
        }

        // If there is no template item, we do not allow hoppers to consume our
        // result. This makes things a bit simpler and avoids a host of UX complications.
        val recipe = getCurrentRecipe() ?: return false

        if (slot == RESULT_SLOT) {
            return !items[RESULT_SLOT].isEmpty
        } else {
            // Crafting slots can send items out that don't match the current recipe.
            // Typically, these would be remaining items (empty bottles, etc).
            val ghost = AutoCrafterGhostRecipe(recipe, FIRST_CRAFTING_SLOT, 3, 3)
            val ingredient = ghost.ingredients[slot] ?: return true
            return !ingredient.test(itemStack)
        }
    }

    fun itemRemovedByHopper(slot: Int) {
        if (slot == RESULT_SLOT) {
            //LOGGER.info("A HOPPER TOOK MY RESULT!")
            craftingResultRemoved()
            updateResultSlot(level!!)
        }
    }

    override fun clearContent() {
        items.clear()
    }

    override fun getContainerSize(): Int {
        return items.size
    }

    override fun isEmpty(): Boolean {
        return true // todo uh, fix this
    }

    override fun getItem(slotNumber: Int): ItemStack {
        return items[slotNumber]
    }

    override fun removeItem(slotNumber: Int, qty: Int): ItemStack {
        val result = ContainerHelper.removeItem(items, slotNumber, qty)
        //LOGGER.info("REMOVING ITEM $slotNumber qty $qty, result is $result")
        return result
    }

    override fun removeItemNoUpdate(i: Int): ItemStack {
        return ContainerHelper.takeItem(items, i)
    }

    override fun setItem(slotNumber: Int, itemStack: ItemStack) {
        //LOGGER.info("SETTING ITEM $slotNumber to $itemStack")
        items[slotNumber] = itemStack

        if (slotNumber == TEMPLATE_SLOT) {
            val all = recipeFinder.allRecipesForItem(itemStack)
            // todo update our selected recipe by finding a match and using the first one
            if (all.isNotEmpty()) {
                currentRecipeIdentifier = all[0].id
            }
            //LOGGER.info("TEMPLATE UPDATED current recipe is now $currentRecipeIdentifier")
        }
    }

    override fun setChanged() {
        super.setChanged()
        //LOGGER.info("CHANGED isClient: ${level!!.isClientSide}")

        updateResultSlot(level!!)
    }

    override fun stillValid(player: Player): Boolean {
        return Container.stillValidBlockEntity(this, player)
    }

    override fun getDefaultName(): Component {
        return Component.literal("Auto Crafter")
    }

    override fun createMenu(i: Int, inventory: Inventory): AbstractContainerMenu {
        return AutoCrafterMenu(
            i, inventory, this,
            ContainerLevelAccess.create(level!!, blockPos),
            dataAccess
        )
    }

    override fun craftingContainer(): CraftingContainer {
        return ProxiedCraftingContainer(this, FIRST_CRAFTING_SLOT)
    }

    override fun canPickup(slotNo: Int): Boolean {
        return true
    }

    override fun load(compoundTag: CompoundTag) {
        super.load(compoundTag)
        items = NonNullList.withSize(this.containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(compoundTag, items)
        if (compoundTag.contains("recipe", 8)) {
            currentRecipeIdentifier = ResourceLocation(compoundTag.getString("recipe"))
        }
    }

    override fun saveAdditional(compoundTag: CompoundTag) {
        super.saveAdditional(compoundTag)
        ContainerHelper.saveAllItems(compoundTag, items)
        if (currentRecipeIdentifier != null) {
            compoundTag.putString("recipe", currentRecipeIdentifier.toString())
        }
    }

    private fun getCurrentRecipe(): RecipeHolder<*>? {
        return currentRecipeIdentifier?.let {
            level!!.recipeManager.byKey(it).getOrNull()
        } ?: run {
            null
        }
    }

    // This is called when destroying the block. It removes the result so we don't drop it since it's not realized yet
    fun prepareForBlockRemoval(): Container {
        removeItemNoUpdate(0)
        return this
    }

    // Used to determine if the current grid can finally produce something.
    // If so, we put an item in the result slot.
    private fun updateResultSlot(
        level: Level
    ) {
        if (level.isClientSide) {
            return
        }
        var itemStack = ItemStack.EMPTY
        val proxy = ProxiedCraftingContainer(this, FIRST_CRAFTING_SLOT)
        val optional = level.server!!.recipeManager.getRecipeFor(RecipeType.CRAFTING, proxy, level)
        if (optional.isPresent) {
            val templateRecipe = getCurrentRecipe()
            val craftIt = if (templateRecipe != null) {
                optional.get().id == templateRecipe.id
            } else {
                true
            }
            if (craftIt) {
                var itemStack2: ItemStack?
                val recipeHolder = optional.get()
                val craftingRecipe = recipeHolder.value()
                if (/*resultContainer.setRecipeUsed(level, serverPlayer, recipeHolder) && */craftingRecipe.assemble(
                        proxy,
                        level.registryAccess()
                    ).also {
                        itemStack2 = it
                    }.isItemEnabled(level.enabledFeatures())
                ) {
                    itemStack = itemStack2
                }
            }
        }

        setItem(RESULT_SLOT, itemStack)
        lastItemStack = itemStack.copy()
        resultWasJustSet = true
    }

    fun craftingResultRemoved() {
        updateGridAfterResultRemoval()
    }

    // After the result is emptied, we need to clear the crafting
    // grid, possibly putting 'remaining items' into the grid. For example,
    // when crafting a honey block, the player is left with bottles. Normally,
    // these might have to go into their inventory or be dropped as there are
    // still items to be crafted. But in our case, when pulling out items via
    // a hopper, we can just replace the original items as we only pull 1 per
    // slot.
    private fun updateGridAfterResultRemoval() {
        //LOGGER.info("Updating grid after result removal")
        lastItemStack = ItemStack.EMPTY
        val proxy = ProxiedCraftingContainer(this, FIRST_CRAFTING_SLOT)

        val nonNullList: NonNullList<ItemStack> = level!!.recipeManager
            .getRemainingItemsFor<CraftingContainer, CraftingRecipe>(
                RecipeType.CRAFTING,
                proxy,
                level!!
            )
        for (i in nonNullList.indices) {
            var currentItem = proxy.getItem(i)
            val remainingItem = nonNullList[i]

            // Reduce the count of the item by 1 if it's there
            if (!currentItem.isEmpty) {
                proxy.removeItem(i, 1)
                currentItem = proxy.getItem(i)
            }

            // If there isn't a remainingItem, just move on
            if (remainingItem.isEmpty) continue

            // If the current item stack is empty, we can replace
            // it with the remaining item.
            if (currentItem.isEmpty) {
                proxy.setItem(i, remainingItem)
                continue
            }

            // If we are here, something is not right.
            //LOGGER.info("Item $i was not empty and we have a remaining item?")
        }
    }

    fun getDispensableSlots(): List<Int> {
        val result = mutableListOf<Int>()

        //LOGGER.info("getDispensableSlots: enter")
        // no recipe, no dispensing
        val recipe = getCurrentRecipe() ?: return result

        if (items[RESULT_SLOT].isEmpty.not()) {
            result.add(RESULT_SLOT)
        } else {
            // look for any items that are not part of the recipe
            val ghost = AutoCrafterGhostRecipe(recipe, FIRST_CRAFTING_SLOT, 3, 3)
            for (i in FIRST_CRAFTING_SLOT .. LAST_CRAFTING_SLOT) {
                val itemStack = items[i]
                if (itemStack.isEmpty) continue

                val ingredient = ghost.ingredients[i]
                if (ingredient == null || !ingredient.test(itemStack)) {
                    result.add(i)
                }
            }
        }
        //LOGGER.info("getDispensableSlots: Returning $result")
        return result
    }

    companion object {
        private const val FIRST_CRAFTING_SLOT = 2
        private const val LAST_CRAFTING_SLOT = 10
        const val RESULT_SLOT = 0
        const val TEMPLATE_SLOT = 1
        private const val CONTAINER_SIZE = 11 // grid + template + result
        private val CRAFTING_SLOTS = (FIRST_CRAFTING_SLOT..LAST_CRAFTING_SLOT).toList().toIntArray()
        private val LOGGER = LogUtils.getLogger()
    }
}

