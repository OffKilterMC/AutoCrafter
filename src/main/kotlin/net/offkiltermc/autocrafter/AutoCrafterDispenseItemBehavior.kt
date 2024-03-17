package net.offkiltermc.autocrafter

import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

// This is a copy of the generic dispense item behavior. It exists solely because
// the standard BlockSource assumes there will be a dispenser block entity in it.
// The generic behavior does not use it, but I don't feel right faking some value
// just to make it happy, so we're just duplicating it and using our own, more
// generic block source.
class AutoCrafterDispenseItemBehavior {
    fun dispense(blockSource: GenericBlockSource<AutoCrafterBlockEntity>, itemStack: ItemStack): ItemStack {
        val result = execute(blockSource, itemStack)
        playSound(blockSource)
        playAnimation(blockSource, blockSource.state.getValue(AutoCrafterDropperBlock.FACING))
        return result
    }

    private fun getDispensePosition(blockSource: GenericBlockSource<AutoCrafterBlockEntity>): Vec3 {
        val direction: Direction = blockSource.state.getValue<Direction>(AutoCrafterDropperBlock.FACING)
        return blockSource.center()
            .add(0.7 * direction.stepX.toDouble(), 0.7 * direction.stepY.toDouble(), 0.7 * direction.stepZ.toDouble())

    }

    private fun execute(blockSource: GenericBlockSource<AutoCrafterBlockEntity>, itemStack: ItemStack): ItemStack {
        val direction = blockSource.state.getValue(AutoCrafterDropperBlock.FACING)
        val position = getDispensePosition(blockSource)
        val itemStack2 = itemStack.copy()
        spawnItem(blockSource.level, itemStack2, 6, direction, position)
        return ItemStack.EMPTY
    }

    private fun playSound(blockSource: GenericBlockSource<AutoCrafterBlockEntity>) {
        blockSource.level.levelEvent(1000, blockSource.pos, 0)
    }

    private fun playAnimation(blockSource: GenericBlockSource<AutoCrafterBlockEntity>, direction: Direction) {
        blockSource.level.levelEvent(2000, blockSource.pos, direction.get3DDataValue())
    }

    companion object {
        fun spawnItem(level: Level, itemStack: ItemStack, i: Int, direction: Direction, position: Position) {
            val x = position.x()
            var y = position.y()
            val z = position.z()
            y = if (direction.axis === Direction.Axis.Y) 0.125.let { y -= it; y } else 0.15625.let { y -= it; y }
            val itemEntity = ItemEntity(level, x, y, z, itemStack)
            val rand = level.random.nextDouble() * 0.1 + 0.2
            itemEntity.setDeltaMovement(
                level.random.triangle(direction.stepX.toDouble() * rand, 0.0172275 * i.toDouble()),
                level.random.triangle(0.2, 0.0172275 * i.toDouble()),
                level.random.triangle(direction.stepZ.toDouble() * rand, 0.0172275 * i.toDouble())
            )
            level.addFreshEntity(itemEntity)
        }
    }
}

