package net.offkiltermc.autocrafter

import com.google.common.collect.Lists
import com.mojang.logging.LogUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.util.StringRepresentable
import net.minecraft.world.*
import net.minecraft.world.entity.player.Player
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
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import net.offkiltermc.autocrafter.AutoCrafter.Companion.AUTO_CRAFTER_BLOCK_ENTITY
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors


open class AutoCrafterBlock(properties: Properties) : Block(properties),
    EntityBlock {

    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity {
        return AutoCrafterBlockEntity(blockPos, blockState, false)
    }

    private fun openContainer(level: Level, blockPos: BlockPos, player: Player) {
        val blockEntity = level.getBlockEntity(blockPos)
        if (blockEntity is AutoCrafterBlockEntity) {
            player.openMenu(blockEntity as MenuProvider?)
            //player.awardStat(...)
        }
    }

    @Deprecated("bad")
    override fun use(
        blockState: BlockState,
        level: Level,
        blockPos: BlockPos,
        player: Player,
        interactionHand: InteractionHand,
        blockHitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }
        this.openContainer(level, blockPos, player)
        return InteractionResult.CONSUME
    }

    @Deprecated("bad")
    override fun onRemove(
        blockState: BlockState,
        level: Level,
        blockPos: BlockPos,
        blockState2: BlockState,
        bl: Boolean
    ) {
        if (blockState.`is`(blockState2.block)) {
            return
        }
        val blockEntity = level.getBlockEntity(blockPos)
        if (blockEntity is AutoCrafterBlockEntity) {
            if (level is ServerLevel) {
                Containers.dropContents(level, blockPos, blockEntity.prepareForBlockRemoval())
            }
            super.onRemove(blockState, level, blockPos, blockState2, bl)
            level.updateNeighbourForOutputSignal(blockPos, this)
        } else {
            super.onRemove(blockState, level, blockPos, blockState2, bl)
        }
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}