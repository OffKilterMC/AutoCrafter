package net.offkiltermc.autocrafter.client

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.offkiltermc.autocrafter.AutoCrafter.Companion.AUTO_CRAFTER_MENU
import net.offkiltermc.autocrafter.AutoCrafterMenu

class AutoCrafterClient : ClientModInitializer {
    override fun onInitializeClient() {
        MenuScreens.register(
            AUTO_CRAFTER_MENU
        ) { containerMenu: AutoCrafterMenu, inventory: Inventory, component: Component ->
            AutoCrafterScreen(
                containerMenu,
                inventory,
                component
            )
        }
    }
}
