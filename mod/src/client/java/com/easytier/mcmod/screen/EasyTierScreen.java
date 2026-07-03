package com.easytier.mcmod.screen;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.easytier.EasyTierCli;
import com.easytier.mcmod.easytier.EasyTierProcess;
import com.easytier.mcmod.easytier.NativeLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * In-game monitoring panel — shows EasyTier status, peers, routes, logs.
 * Config/download/setup is handled by EasyTierConfigScreen on the title screen.
 */
public class EasyTierScreen extends Screen {
    private static final int TAB_W = 68;
    private static final int CONTENT_X = 8;
    private static final int CONTENT_Y = 32;

    private enum Tab { STATUS, PEERS, ROUTES, LOGS }
    private Tab currentTab = Tab.STATUS;

    private String statusText = "Loading...";
    private List<String> peerLines = new ArrayList<>();
    private List<String> routeLines = new ArrayList<>();
    private List<String> logLines = new ArrayList<>();

    private int scrollOffset = 0;

    public EasyTierScreen() {
        super(Component.literal("EasyTier"));
    }

    @Override
    protected void init() {
        // Tab bar
        int tx = 4;
        tab(tx, "Status", Tab.STATUS); tx += TAB_W + 1;
        tab(tx, "Peers", Tab.PEERS); tx += TAB_W + 1;
        tab(tx, "Routes", Tab.ROUTES); tx += TAB_W + 1;
        tab(tx, "Logs", Tab.LOGS); tx += TAB_W + 1;

        // Refresh
        addRenderableWidget(Button.builder(Component.literal("↻"), b -> refreshData())
                .bounds(this.width - 26, 6, 20, 20).build());

        // Scroll
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(this.width - 26, this.height - 52, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(this.width - 26, this.height - 30, 20, 20).build());

        // Done
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());

        refreshData();
    }

    private void tab(int x, String label, Tab tab) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            currentTab = tab; scrollOffset = 0; refreshData();
        }).bounds(x, 6, TAB_W, 20).build());
    }

    private void scroll(int dir) {
        scrollOffset = Math.max(0, scrollOffset + dir);
    }

    private void refreshData() {
        var config = EasyTierMod.getConfig();

        switch (currentTab) {
            case STATUS -> CompletableFuture.runAsync(() ->
                EasyTierCli.executeJson(config, "node", "info").thenAccept(json -> {
                    if (json instanceof JsonObject obj) {
                        var sb = new StringBuilder();
                        sb.append("§eEasyTier ").append(NativeLoader.getCurrentVersion()).append("\n");
                        append(obj, sb, "Virtual IP"); append(obj, sb, "Hostname");
                        append(obj, sb, "Peer ID"); append(obj, sb, "STUN Type");
                        append(obj, sb, "Public IPv4"); append(obj, sb, "Public IPv6");
                        var proc = EasyTierMod.getEasyTierProcess();
                        sb.append("\nProcess: ").append(proc != null && proc.isRunning() ? "§aRunning" : "§cStopped");
                        statusText = sb.toString();
                    }
                }).exceptionally(e -> { statusText = "§c" + e.getMessage(); return null; })
            );

            case PEERS -> CompletableFuture.runAsync(() ->
                EasyTierCli.executeJson(config, "peer", "list").thenAccept(json -> {
                    peerLines.clear();
                    if (json instanceof JsonArray arr) {
                        if (arr.isEmpty()) peerLines.add("No peers connected");
                        else {
                            peerLines.add(String.format("%-16s %-18s %6s %5s %5s", "IP","Hostname","Lat","Loss","NAT"));
                            for (JsonElement el : arr) {
                                if (el instanceof JsonObject p)
                                    peerLines.add(String.format("%-16s %-18s %6s %5s %5s",
                                            s(p,"cidr",16), s(p,"hostname",18),
                                            s(p,"latMs",6), s(p,"lossRate",5), s(p,"natType",5)));
                            }
                        }
                    }
                }).exceptionally(e -> { peerLines.add("§c"+e.getMessage()); return null; })
            );

            case ROUTES -> CompletableFuture.runAsync(() ->
                EasyTierCli.executeJson(config, "route", "list").thenAccept(json -> {
                    routeLines.clear();
                    if (json instanceof JsonArray arr) {
                        if (arr.isEmpty()) routeLines.add("No routes");
                        else {
                            routeLines.add(String.format("%-16s %-16s %-16s %4s", "Target","NextHop","Host","Hops"));
                            for (JsonElement el : arr) {
                                if (el instanceof JsonObject r)
                                    routeLines.add(String.format("%-16s %-16s %-16s %4s",
                                            s(r,"ipv4",16), s(r,"nextHopIpv4",16),
                                            s(r,"hostname",16), s(r,"pathLen",4)));
                            }
                        }
                    }
                }).exceptionally(e -> { routeLines.add("§c"+e.getMessage()); return null; })
            );

            case LOGS -> {
                var proc = EasyTierMod.getEasyTierProcess();
                logLines = (proc != null && proc.isRunning())
                        ? proc.getRecentLogs(100)
                        : List.of("EasyTier not running.");
            }
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredString(this.font, "EasyTier Monitor", this.width / 2, 3, 0xFFFFFF);

        int y = CONTENT_Y;
        switch (currentTab) {
            case STATUS -> renderLines(ctx, y, statusText.split("\n"));
            case PEERS -> renderLines(ctx, y, peerLines.toArray(new String[0]));
            case ROUTES -> renderLines(ctx, y, routeLines.toArray(new String[0]));
            case LOGS -> renderLines(ctx, y, logLines.toArray(new String[0]));
        }
    }

    private void renderLines(GuiGraphics ctx, int startY, String[] lines) {
        int y = startY;
        int maxVis = (this.height - startY - 60) / 12;
        int from = Math.max(0, Math.min(scrollOffset, Math.max(0, lines.length - maxVis)));
        for (int i = from; i < Math.min(lines.length, from + maxVis); i++) {
            String line = lines[i].replaceAll("§[0-9a-fA-F]", "");
            int color = lines[i].startsWith("§c") ? 0xFF5555 : lines[i].startsWith("§a") ? 0x55FF55
                    : lines[i].startsWith("§e") ? 0xFFFF55 : 0xCCCCCC;
            ctx.drawString(this.font, line, CONTENT_X, y, color);
            y += 12;
        }
        if (lines.length == 0) ctx.drawString(this.font, "Loading...", CONTENT_X, y, 0x888888);
    }

    @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(null); }
    @Override public boolean isPauseScreen() { return false; }

    private static void append(JsonObject obj, StringBuilder sb, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull())
            sb.append(key).append(": ").append(obj.get(key).getAsString()).append("\n");
    }
    private static String s(JsonObject obj, String key, int max) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "-";
        String v = obj.get(key).getAsString();
        return v.length() > max ? v.substring(0, max) : v;
    }
}
