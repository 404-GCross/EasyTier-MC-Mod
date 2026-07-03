package com.easytier.mcmod.mixin.client;

import com.easytier.mcmod.easytier.NativeLoader;
import com.easytier.mcmod.screen.EasyTierConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds an "EasyTier" button to the title screen so users can set up
 * their VPN before joining a server.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    private static boolean firstRunShown = false;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addEasyTierButton(CallbackInfo ci) {
        // Auto-open setup once per launch if EasyTier is not installed
        if (!NativeLoader.isInstalled() && !firstRunShown && this.minecraft != null) {
            firstRunShown = true;
            this.minecraft.setScreen(new EasyTierConfigScreen(null));
            return;
        }

        // Add EasyTier button next to the existing buttons
        int x = this.width / 2 + 104;
        int y = this.height / 4 + 48;

        this.addRenderableWidget(Button.builder(
                Component.literal("EasyTier"),
                btn -> this.minecraft.setScreen(new EasyTierConfigScreen(null))
        ).bounds(x, y, 72, 20).build());
    }
}
