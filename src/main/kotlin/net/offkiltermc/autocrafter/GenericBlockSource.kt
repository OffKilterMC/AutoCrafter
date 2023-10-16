package net.offkiltermc.autocrafter

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

// A block source with type-erased entity
data class GenericBlockSource<T: BlockEntity>(
    val level: ServerLevel,
    val pos: BlockPos,
    val state: BlockState,
    val blockEntity: T,
) {
    fun center(): Vec3 {
        return pos.center
    }
}


