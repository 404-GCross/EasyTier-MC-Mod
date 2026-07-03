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
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main management GUI for EasyTier.
 * Tab-based screen showing Status, Peers, Routes, and Logs.
 */
public class EasyTierScreen extends Screen {
    private static final int TAB_WIDTH = 80;
    private static final int CONTENT_Y = 35;
    private static final int CONTENT_X = 10;

    private enum Tab { STATUS, PEERS, ROUTES, LOGS, CONFIG }
    private Tab currentTab = Tab.STATUS;

    // Cached data
    private String statusText = "Loading...";
    private List<String> peerLines = new ArrayList<>();
    private List<String> routeLines = new ArrayList<>();
    private List<String> logLines = new ArrayList<>();

    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_LINES = 20;

    public EasyTierScreen() {
        super(Component.translatable("screen.easytier-mcmod.main"));
    }

    @Override
    protected void init() {
        int tabY = 10;
        int tabX = 5;

        // Tab buttons
        addRenderableWidget(Button.builder(
                Component.literal("Status"),
                btn -> switchTab(Tab.STATUS)
        ).bounds(tabX, tabY, TAB_WIDTH, 20).build());
        tabX += TAB_WIDTH + 2;

        addRenderableWidget(Button.builder(
                Component.literal("Peers"),
                btn -> switchTab(Tab.PEERS)
        ).bounds(tabX, tabY, TAB_WIDTH, 20).build());
        tabX += TAB_WIDTH + 2;

        addRenderableWidget(Button.builder(
                Component.literal("Routes"),
                btn -> switchTab(Tab.ROUTES)
        ).bounds(tabX, tabY, TAB_WIDTH, 20).build());
        tabX += TAB_WIDTH + 2;

        addRenderableWidget(Button.builder(
                Component.literal("Logs"),
                btn -> switchTab(Tab.LOGS)
        ).bounds(tabX, tabY, TAB_WIDTH, 20).build());
        tabX += TAB_WIDTH + 2;

        addRenderableWidget(Button.builder(
                Component.literal("Config"),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new EasyTierConfigScreen(this));
                    }
                }
        ).bounds(tabX, tabY, TAB_WIDTH, 20).build());
        tabX += TAB_WIDTH + 2;

        // Refresh button
        addRenderableWidget(Button.builder(
                Component.literal("↻"),
                btn -> refreshData()
        ).bounds(this.width - 30, 10, 20, 20).build());

        // Scroll buttons
        addRenderableWidget(Button.builder(
                Component.literal("▲"),
                btn -> { scrollOffset = Math.max(0, scrollOffset - 1); }
        ).bounds(this.width - 30, this.height - 60, 20, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("▼"),
                btn -> { scrollOffset++; }
        ).bounds(this.width - 30, this.height - 35, 20, 20).build());

        // Done button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> onClose()
        ).bounds(this.width / 2 - 50, this.height - 25, 100, 20).build());

        refreshData();
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        scrollOffset = 0;
        refreshData();
    }

    private void refreshData() {
        var config = EasyTierMod.getConfig();

        switch (currentTab) {
            case STATUS -> CompletableFuture.runAsync(() -> {
                EasyTierCli.executeJson(config, "node", "info").thenAccept(json -> {
                    if (json instanceof JsonObject obj) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("EasyTier Version: ").append(NativeLoader.getCurrentVersion()).append("\n");
                        appendIfPresent(obj, sb, "Virtual IP", "virtual_ip");
                        appendIfPresent(obj, sb, "Hostname", "hostname");
                        appendIfPresent(obj, sb, "Peer ID", "peer_id");
                        appendIfPresent(obj, sb, "STUN Type", "stun_type");
                        appendIfPresent(obj, sb, "Public IPv4", "public_ipv4");
                        appendIfPresent(obj, sb, "Public IPv6", "public_ipv6");
                        sb.append("\n");

                        var proc = EasyTierMod.getEasyTierProcess();
                        sb.append("Process: ").append(proc != null && proc.isRunning() ? "§aRunning" : "§cStopped");
                        statusText = sb.toString();
                    }
                }).exceptionally(e -> {
                    statusText = "§cError: " + e.getMessage() + "\n\n§7Is EasyTier running?";
                    return null;
                });
            });

            case PEERS -> CompletableFuture.runAsync(() -> {
                EasyTierCli.executeJson(config, "peer", "list").thenAccept(json -> {
                    peerLines.clear();
                    if (json instanceof JsonArray arr && arr.isEmpty()) {
                        peerLines.add("No peers connected");
                    } else if (json instanceof JsonArray arr) {
                        peerLines.add(String.format("%-18s %-20s %8s %6s %6s",
                                "IP", "Hostname", "Lat(ms)", "Loss%", "NAT"));
                        peerLines.add("-".repeat(70));
                        for (JsonElement el : arr) {
                            if (el instanceof JsonObject p) {
                                peerLines.add(String.format("%-18s %-20s %8s %6s %6s",
                                        str(p, "cidr", 18),
                                        str(p, "hostname", 20),
                                        str(p, "latMs", 8),
                                        str(p, "lossRate", 6),
                                        str(p, "natType", 6)));
                            }
                        }
                    }
                }).exceptionally(e -> {
                    peerLines.clear();
                    peerLines.add("§cError: " + e.getMessage());
                    return null;
                });
            });

            case ROUTES -> CompletableFuture.runAsync(() -> {
                EasyTierCli.executeJson(config, "route", "list").thenAccept(json -> {
                    routeLines.clear();
                    if (json instanceof JsonArray arr && arr.isEmpty()) {
                        routeLines.add("No routes available");
                    } else if (json instanceof JsonArray arr) {
                        routeLines.add(String.format("%-18s %-20s %-18s %6s %8s",
                                "Target IP", "Hostname", "Next Hop", "Hops", "Latency"));
                        routeLines.add("-".repeat(80));
                        for (JsonElement el : arr) {
                            if (el instanceof JsonObject r) {
                                routeLines.add(String.format("%-18s %-20s %-18s %6s %8s",
                                        str(r, "ipv4", 18),
                                        str(r, "hostname", 20),
                                        str(r, "nextHopIpv4", 18),
                                        str(r, "pathLen", 6),
                                        str(r, "pathLatency", 8)));
                            }
                        }
                    }
                }).exceptionally(e -> {
                    routeLines.clear();
                    routeLines.add("§cError: " + e.getMessage());
                    return null;
                });
            });

            case LOGS -> {
                EasyTierProcess proc = EasyTierMod.getEasyTierProcess();
                if (proc != null && proc.isRunning()) {
                    logLines = proc.getRecentLogs(100);
                } else {
                    logLines = List.of("EasyTier is not running. Start it from the Config tab.");
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFF);

        int y = CONTENT_Y;

        switch (currentTab) {
            case STATUS -> renderStatus(context, y);
            case PEERS -> renderLines(context, y, peerLines);
            case ROUTES -> renderLines(context, y, routeLines);
            case LOGS -> renderLines(context, y, logLines);
            case CONFIG -> {
                // Config tab just shows quick summary; full edit via Config button
                var c = EasyTierMod.getConfig();
                context.drawString(this.font, "Network: " + c.networkName, CONTENT_X, y, 0xAAAAAA);
                y += 12;
                context.drawString(this.font, "RPC: " + c.rpcHost + ":" + c.rpcPort, CONTENT_X, y, 0xAAAAAA);
                y += 12;
                context.drawString(this.font, "Protocol: " + c.defaultProtocol, CONTENT_X, y, 0xAAAAAA);
                y += 12;
                context.drawString(this.font, "Auto-start: " + (c.autoStart ? "Yes" : "No"), CONTENT_X, y, 0xAAAAAA);
                y += 12;
                context.drawString(this.font, "Encryption: " + (c.enableEncryption ? "On" : "Off"), CONTENT_X, y, 0xAAAAAA);
                y += 12;
                context.drawString(this.font, "", CONTENT_X, y, 0xAAAAAA);
                y += 12;
                context.drawString(this.font, "Click 'Config' tab button to edit all settings", CONTENT_X, y, 0xFFAA00);
            }
        }
    }

    private void renderStatus(GuiGraphics context, int y) {
        String[] lines = statusText.split("\n");
        for (String line : lines) {
            context.drawString(this.font, line, CONTENT_X, y, 0xCCCCCC);
            y += 12;
        }
    }

    private void renderLines(GuiGraphics context, int startY, List<String> lines) {
        int y = startY;
        int maxLines = (this.height - startY - 70) / 12;
        int fromIndex = Math.max(0, Math.min(scrollOffset, Math.max(0, lines.size() - maxLines)));

        for (int i = fromIndex; i < Math.min(lines.size(), fromIndex + maxLines); i++) {
            String line = lines.get(i);
            // Handle color codes
            int color = 0xCCCCCC;
            if (line.startsWith("§c")) color = 0xFF5555;
            else if (line.startsWith("§a")) color = 0x55FF55;
            else if (line.startsWith("§e")) color = 0xFFFF55;
            else if (line.startsWith("§7")) color = 0x888888;

            String displayLine = line.replaceAll("§[0-9a-f]", "");
            context.drawString(this.font, displayLine, CONTENT_X, y, color);
            y += 12;
        }

        if (lines.isEmpty()) {
            context.drawString(this.font, "Loading...", CONTENT_X, y, 0x888888);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Helper methods
    private static void appendIfPresent(JsonObject obj, StringBuilder sb, String label, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            sb.append(label).append(": ").append(obj.get(key).getAsString()).append("\n");
        }
    }

    private static String str(JsonObject obj, String key, int maxLen) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return pad("-", maxLen);
        String val = obj.get(key).getAsString();
        return pad(val.length() > maxLen ? val.substring(0, maxLen) : val, maxLen);
    }

    private static String pad(String s, int len) {
        if (s.length() >= len) return s;
        return s + " ".repeat(len - s.length());
    }
}
