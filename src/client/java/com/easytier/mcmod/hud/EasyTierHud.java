package com.easytier.mcmod.hud;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.config.ModConfig;
import com.easytier.mcmod.easytier.NativeLoader;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders a compact EasyTier network status HUD overlay.
 */
public class EasyTierHud {

    private static String statusText = "EasyTier: ...";
    private static String peerCountText = "";
    private static String versionText = "";
    private static long lastUpdate = 0;
    private static final long UPDATE_INTERVAL_MS = 3000;

    public static void register() {
        HudRenderCallback.EVENT.register((GuiGraphics context, DeltaTracker delta) -> {
            ModConfig config = EasyTierMod.getConfig();
            if (config == null || !config.hudEnabled) return;

            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;
            if (client.options.hideGui) return;

            // Periodic refresh (every 3 seconds)
            long now = System.currentTimeMillis();
            if (now - lastUpdate > UPDATE_INTERVAL_MS) {
                lastUpdate = now;
                refreshStatus();
            }

            // Position
            int x, y;
            int margin = 5;
            int lineHeight = 12;
            int hudWidth = 200;

            switch (config.hudPosition) {
                case "top_left" -> {
                    x = margin;
                    y = margin;
                }
                case "bottom_left" -> {
                    x = margin;
                    y = client.getWindow().getGuiScaledHeight() - margin - lineHeight * 4;
                }
                case "bottom_right" -> {
                    x = client.getWindow().getGuiScaledWidth() - hudWidth - margin;
                    y = client.getWindow().getGuiScaledHeight() - margin - lineHeight * 4;
                }
                default -> { // top_right
                    x = client.getWindow().getGuiScaledWidth() - hudWidth - margin;
                    y = margin;
                }
            }

            int color = config.hudColor;

            // Background
            context.fill(x - 2, y - 2, x + hudWidth + 2, y + lineHeight * 4 + 2, 0x88000000);

            // Lines
            context.drawString(client.font, statusText, x, y, color);
            y += lineHeight;
            context.drawString(client.font, peerCountText, x, y, color);
            y += lineHeight;
            context.drawString(client.font, versionText, x, y, 0x888888);
        });
    }

    private static void refreshStatus() {
        var config = EasyTierMod.getConfig();
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();

        if (running) {
            statusText = "EasyTier: §aRunning";
        } else {
            statusText = "EasyTier: §cStopped";
        }

        peerCountText = "";
        versionText = "v" + NativeLoader.getCurrentVersion();

        // Async fetch peer count
        if (running) {
            com.easytier.mcmod.easytier.EasyTierCli.executeJson(config, "peer", "list").thenAccept(json -> {
                if (json instanceof com.google.gson.JsonArray arr) {
                    peerCountText = "Peers: " + arr.size();
                }
            }).exceptionally(e -> {
                peerCountText = "Peers: ?";
                return null;
            });
        } else {
            peerCountText = "";
        }
    }
}
