package com.easytier.mcmod;

import com.easytier.mcmod.config.ModConfig;
import com.easytier.mcmod.hud.EasyTierHud;
import com.easytier.mcmod.screen.EasyTierScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class EasyTierModClient implements ClientModInitializer {
    private static KeyMapping openScreenKey;
    private static KeyMapping toggleHudKey;

    @Override
    public void onInitializeClient() {
        // Register keybindings
        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.easytier-mcmod.open_screen",
                GLFW.GLFW_KEY_O,
                "category.easytier-mcmod.main"
        ));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.easytier-mcmod.toggle_hud",
                GLFW.GLFW_KEY_H,
                "category.easytier-mcmod.main"
        ));

        // Register tick handler for keybindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new EasyTierScreen());
            }
            while (toggleHudKey.consumeClick()) {
                ModConfig config = EasyTierMod.getConfig();
                config.hudEnabled = !config.hudEnabled;
                // Save immediately
                config.save(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
            }
        });

        // Register HUD overlay
        EasyTierHud.register();
    }

    public static KeyMapping getOpenScreenKey() {
        return openScreenKey;
    }
}
