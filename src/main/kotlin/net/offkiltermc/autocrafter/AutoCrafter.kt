package net.offkiltermc.autocrafter

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor

class AutoCrafter : ModInitializer {
    override fun onInitialize() {
        Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation("autocrafter", "auto_crafter"),
            AUTO_CRAFTER_BLOCK,
        )
        Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation("autocrafter", "auto_crafter_dropper"),
            AUTO_CRAFTER_DROPPER_BLOCK,
        )
        Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation("autocrafter", "auto_crafter"),
            AUTO_CRAFTER_ITEM
        )
        Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation("autocrafter", "auto_crafter_dropper"),
            AUTO_CRAFTER_DROPPER_ITEM
        )
        Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation("autocrafter", "auto_crafter"),
            AUTO_CRAFTER_MENU
        )
        AUTO_CRAFTER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation("autocrafter", "auto_crafter"),
            BlockEntityType.Builder.of({ pos: BlockPos?, state: BlockState? ->
                AutoCrafterBlockEntity(
                    pos!!, state!!, false
                )
            }, AUTO_CRAFTER_BLOCK).build(null)
        )
        AUTO_CRAFTER_DROPPER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation("autocrafter", "auto_crafter_dropper"),
            BlockEntityType.Builder.of({ pos: BlockPos?, state: BlockState? ->
                AutoCrafterBlockEntity(
                    pos!!, state!!, true
                )
            }, AUTO_CRAFTER_BLOCK).build(null)
        )
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
            .register(ModifyEntries { content: FabricItemGroupEntries ->
                content.accept(
                    AUTO_CRAFTER_ITEM, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                )
                content.accept(
                    AUTO_CRAFTER_DROPPER_ITEM, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                )
            })
    }

    companion object {
        val AUTO_CRAFTER_BLOCK: Block = AutoCrafterBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5f)
                .sound(SoundType.WOOD)
        )
        val AUTO_CRAFTER_DROPPER_BLOCK: Block = AutoCrafterDropperBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5f)
                .sound(SoundType.WOOD)
        )
        val AUTO_CRAFTER_ITEM = BlockItem(AUTO_CRAFTER_BLOCK, FabricItemSettings())
        val AUTO_CRAFTER_DROPPER_ITEM = BlockItem(AUTO_CRAFTER_DROPPER_BLOCK, FabricItemSettings())
        var AUTO_CRAFTER_BLOCK_ENTITY: BlockEntityType<AutoCrafterBlockEntity>? = null
        var AUTO_CRAFTER_DROPPER_BLOCK_ENTITY: BlockEntityType<AutoCrafterBlockEntity>? = null
        val AUTO_CRAFTER_MENU =
            MenuType({ i: Int, inventory: Inventory -> AutoCrafterMenu(i, inventory) }, FeatureFlags.VANILLA_SET)
    }
}
