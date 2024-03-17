package net.offkiltermc.autocrafter.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.offkiltermc.autocrafter.AutoCrafterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
    @Inject(method="tryTakeInItemFromSlot", at=@At("RETURN"))
    private static void onTryTakeInItem(Hopper hopper, Container container, int i, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof AutoCrafterBlockEntity) {
            if (cir.getReturnValue()) {
                // tell the container that we removed an item via hopper
                ((AutoCrafterBlockEntity)container).itemRemovedByHopper(i);
            }
            ((AutoCrafterBlockEntity)container).hopperRemovalEnded();
        }
    }
    @Inject(method="tryTakeInItemFromSlot", at=@At("HEAD"))
    private static void onTryTakeInItem2(Hopper hopper, Container container, int i, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof AutoCrafterBlockEntity) {
            // tell the container that we are looking to remove an item via hopper
            ((AutoCrafterBlockEntity)container).hopperRemovalStarted();
        }
    }
}
