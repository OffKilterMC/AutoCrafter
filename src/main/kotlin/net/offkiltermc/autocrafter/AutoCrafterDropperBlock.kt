package net.offkiltermc.autocrafter

import com.mojang.logging.LogUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.*
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.offkiltermc.autocrafter.AutoCrafter.Companion.AUTO_CRAFTER_BLOCK_ENTITY


class AutoCrafterDropperBlock(properties: Properties) : AutoCrafterBlock(properties) {
    private var dispenseOnNextTick = false

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING,Direction.NORTH)
                .setValue(TRIGGERED, false)
                .setValue(CRAFTING, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING, TRIGGERED, CRAFTING)
    }

    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity {
        return AutoCrafterBlockEntity(blockPos, blockState, true)
    }

    override fun getStateForPlacement(blockPlaceContext: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(
            FACING,
            blockPlaceContext.nearestLookingDirection.opposite
        )
    }

    @Deprecated("Deprecated in Java")
    override fun hasAnalogOutputSignal(blockState: BlockState): Boolean {
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun getAnalogOutputSignal(blockState: BlockState, level: Level, blockPos: BlockPos): Int {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(blockPos))
    }

    @Deprecated("Deprecated in Java")
    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    @Deprecated("Deprecated in Java")
    override fun rotate(blockState: BlockState, rotation: Rotation): BlockState {
        return blockState.setValue(
            FACING,
            rotation.rotate(blockState.getValue(FACING))
        )
    }

    @Deprecated("Deprecated in Java")
    override fun mirror(blockState: BlockState, mirror: Mirror): BlockState {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)))
    }

    @Deprecated("Deprecated in Java")
    override fun neighborChanged(
        blockState: BlockState,
        level: Level,
        blockPos: BlockPos,
        block: Block,
        blockPos2: BlockPos,
        bl: Boolean
    ) {
        val hasSignal = level.hasNeighborSignal(blockPos) || level.hasNeighborSignal(blockPos.above())
        val isTriggered = blockState.getValue(TRIGGERED)
        if (hasSignal && !isTriggered) {
            level.scheduleTick(blockPos, this, 4)
            level.setBlock(blockPos, blockState.setValue(TRIGGERED, true), 2)
            //LOGGER.info("Triggered")
            dispenseOnNextTick = true
        } else if (!hasSignal && isTriggered) {
            level.setBlock(blockPos, blockState.setValue(TRIGGERED, false), 2)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun tick(
        blockState: BlockState,
        serverLevel: ServerLevel,
        blockPos: BlockPos,
        randomSource: RandomSource
    ) {
        if (blockState.getValue(CRAFTING)) {
            serverLevel.setBlock(blockPos, blockState.setValue(CRAFTING, false), 2)
        }
        if (dispenseOnNextTick) {
            dispenseOnNextTick = false
            this.dispenseFrom(serverLevel, blockState, blockPos)
        }
    }


    private fun dispenseFrom(serverLevel: ServerLevel, blockState: BlockState, blockPos: BlockPos) {
        //LOGGER.info("Attempting to dispense...")
        val blockEntity: AutoCrafterBlockEntity? =
            serverLevel.getBlockEntity(blockPos, AUTO_CRAFTER_BLOCK_ENTITY!!).orElse(null)
        if (blockEntity == null) {
            LOGGER.warn(
                "Ignoring dispensing attempt for AutoCrafter without matching block entity at $blockPos"
            )
            return
        }

        val direction = serverLevel.getBlockState(blockPos).getValue(FACING)
        val container = HopperBlockEntity.getContainerAt(serverLevel, blockPos.relative(direction))
        val blockSource = GenericBlockSource(serverLevel, blockPos, blockState, blockEntity)

        // Always check the result slot
        val result = blockEntity.getItem(AutoCrafterBlockEntity.RESULT_SLOT)
        if (!result.isEmpty && dispenseOneItem(blockEntity, 0, direction, blockSource, blockPos, blockState, serverLevel, container)) {
            // tell the container to update its grid after successful crafting
            blockEntity.craftingResultRemoved()
        }

        // after the above, there may be remaining items (empty bottles, buckets, etc.)
        // or they may be there from a prior attempt, but there was no room in the destination
        // container
        val slots = blockEntity.getDispensableSlots()
        for (slot in slots) {
            dispenseOneItem(blockEntity, slot, direction, blockSource, blockPos, blockState, serverLevel, container)
        }

        blockEntity.setChanged()
    }

    // Gets the item from the given slot and does the low-level dispense. If
    // the item was consumed, shrink it and reset it on the block entity container.
    private fun dispenseOneItem(
        blockEntity: AutoCrafterBlockEntity,
        slot: Int,
        direction: Direction,
        blockSource: GenericBlockSource<AutoCrafterBlockEntity>,
        blockPos: BlockPos,
        blockState: BlockState,
        serverLevel: ServerLevel,
        container: Container?
    ): Boolean {
        // no point in animating if we are pointed at a container
        if (container == null) {
            openMouth(serverLevel, blockPos, blockState)
        }

        // Don't allow mutation of the one the entity holds. It will confuse things.
        // The setItem below needs to see a change.
        val itemStack = blockEntity.getItem(slot).copy()
        val remaining = dispenseOneCore(blockEntity, itemStack, direction, blockSource, container)
        return if (remaining.isEmpty) {
            // item was consumed...
            itemStack.shrink(1)
            blockEntity.setItem(slot, itemStack)
            true
        } else {
            false
        }
    }

    // Does the low-level dispensing or moving into a container. We only pass one item
    // into the appropriate function from a copy (i.e. we do not mutate the input stack
    // at all). Returns empty itemStack if the item was successfully consumed.
    private fun dispenseOneCore(
        blockEntity: AutoCrafterBlockEntity,
        itemStack: ItemStack,
        direction: Direction,
        blockSource: GenericBlockSource<AutoCrafterBlockEntity>,
        container: Container?
    ): ItemStack {
        return if (container == null) {
            DISPENSE_BEHAVIOUR.dispense(blockSource, itemStack.copy().split(1))
        } else {
            HopperBlockEntity.addItem(
                blockEntity,
                container,
                itemStack.copy().split(1),
                direction.opposite
            )
        }
    }

    private fun openMouth(
        level: ServerLevel,
        blockPos: BlockPos,
        blockState: BlockState
    ) {
        if (blockState.getValue(CRAFTING) == false) {
            level.setBlock(blockPos, blockState.setValue(CRAFTING, true), 2)
            level.scheduleTick(blockPos, this, 10)
        }
    }

    companion object {
        val FACING: DirectionProperty = DirectionalBlock.FACING
        val TRIGGERED: BooleanProperty = BlockStateProperties.TRIGGERED
        val CRAFTING: BooleanProperty = BooleanProperty.create("crafting")
        private val LOGGER = LogUtils.getLogger()
        private val DISPENSE_BEHAVIOUR = AutoCrafterDispenseItemBehavior()
    }
}