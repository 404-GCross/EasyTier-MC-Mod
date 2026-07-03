package com.easytier.mcmod;

import com.easytier.mcmod.easytier.NativeLoader;
import com.easytier.mcmod.screen.EasyTierConfigScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Adds EasyTier button to the title screen using Fabric API (no Mixin needed).
 */
public class TitleScreenHandler {
    private static boolean firstRunShown = false;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) return;

            // Auto-open setup once per launch if EasyTier is not installed
            if (!NativeLoader.isInstalled() && !firstRunShown) {
                firstRunShown = true;
                client.setScreen(new EasyTierConfigScreen(null));
                return;
            }

            // Add EasyTier button
            int x = screen.width / 2 + 104;
            int y = screen.height / 4 + 48;

            Screens.getButtons(screen).add(Button.builder(
                    Component.literal("EasyTier"),
                    btn -> client.setScreen(new EasyTierConfigScreen(null))
            ).bounds(x, y, 72, 20).build());
        });
    }
}
