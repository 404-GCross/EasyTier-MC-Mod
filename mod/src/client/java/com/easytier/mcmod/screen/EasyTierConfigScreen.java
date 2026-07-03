package com.easytier.mcmod.screen;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.config.ModConfig;
import com.easytier.mcmod.easytier.EasyTierCli;
import com.easytier.mcmod.easytier.NativeLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class EasyTierConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    private int page;
    private int scrollY, maxScroll;

    private EditBox hostnameField, networkNameField, networkSecretField, peerUrlField;
    private String connectorList = "";
    private long connectorListFetch;

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("easytier.title"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override protected void init() { page = 0; scrollY = 0; build(); }

    private void build() {
        clearWidgets();
        if (page == 0) buildMain();
        else if (page == 1) buildAdvanced();
        else buildCore();
        bottomBar();
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (page == 1) {
            scrollY = (int) Math.clamp(scrollY - v * 20, 0, maxScroll);
            build();
            return true;
        }
        return false;
    }

    // ============ BOTTOM BAR ============
    private void bottomBar() {
        int bot = this.height - 24, bw = 52, gap = 2, cx = this.width / 2;
        // On main page: [Advanced] [Core] [Save&Close] [Close]
        // On subpages: [Back&Save] [Save&Close] [Close]
        if (page == 0) {
            int bx = cx - (bw * 4 + gap * 3) / 2;
            bbtn(bx, bot, bw, t("easytier.advanced"), () -> { page = 1; build(); }); bx += bw + gap;
            bbtn(bx, bot, bw, t("easytier.core"), () -> { page = 2; build(); }); bx += bw + gap;
            bbtn(bx, bot, bw, "Save&Close", this::saveAndClose); bx += bw + gap;
            bbtn(bx, bot, bw, "Cancel", this::onClose);
        } else {
            int bx = cx - (bw * 3 + gap * 2) / 2;
            bbtn(bx, bot, bw, t("easytier.back"), () -> { saveConfig(); page = 0; scrollY = 0; build(); }); bx += bw + gap;
            bbtn(bx, bot, bw, "Save&Close", this::saveAndClose); bx += bw + gap;
            bbtn(bx, bot, bw, "Cancel", this::onClose);
        }
    }

    // ============ MAIN PAGE ============
    private void buildMain() {
        int cx = this.width / 2, y = 26;
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        String ver = NativeLoader.getCurrentVersion();

        // Status header
        int statusColor = !NativeLoader.isInstalled() ? 0xFF5555 : running ? 0x55FF55 : 0xAAAAAA;
        String icon = running ? "●" : "○";
        drawCentered(cx, y, "§7" + icon + " " + (running ? t("easytier.status.running") : NativeLoader.isInstalled() ? t("easytier.status.stopped") : t("easytier.status.not_installed")), statusColor);
        y += 12;
        drawCentered(cx, y, "§8v" + ver, 0x888888);
        y += 18;

        // Separator
        drawHLine(y); y += 6;

        // Fields
        hostnameField = addRow("hostname", config.hostname, cx, y); y += 20;
        networkNameField = addRow("networkName", config.networkName, cx, y); y += 20;
        networkSecretField = addRow("networkSecret", config.networkSecret, cx, y); y += 20;
        peerUrlField = addRow("peerUrl", "", cx, y);

        // Peer buttons inline
        addRenderableWidget(Button.builder(Component.literal("+"), b -> addConnector())
                .bounds(cx + 108, y, 18, 14).build());
        addRenderableWidget(Button.builder(Component.literal(t("easytier.peers")), b -> refreshConnectors())
                .bounds(cx + 128, y, 36, 14).build());
        y += 18;
        addText(cx - 100, y, 0x777777, connectorList);
        y += 18;

        // Big Start/Stop button
        addRenderableWidget(Button.builder(
                Component.literal(running ? "■ " + t("easytier.stop") : "▶ " + t("easytier.start")),
                b -> toggleProcess()
        ).bounds(cx - 55, y, 110, 22).build());
    }

    // ============ ADVANCED PAGE ============
    private void buildAdvanced() {
        int cx = this.width / 2, y = 26 - scrollY;
        drawCentered(cx, y, t("easytier.advanced.title"), 0xFFAA00); y += 16;

        // Flags
        addToggle(cx, y, "encryption", config.enableEncryption, v -> config.enableEncryption = v); y += 17;
        addToggle(cx, y, "ipv6", config.enableIpv6, v -> config.enableIpv6 = v); y += 17;
        addToggle(cx, y, "latency_first", config.latencyFirst, v -> config.latencyFirst = v); y += 17;
        addToggle(cx, y, "kcp_proxy", config.enableKcpProxy, v -> config.enableKcpProxy = v); y += 17;
        addToggle(cx, y, "quic_proxy", config.enableQuicProxy, v -> config.enableQuicProxy = v); y += 17;
        addToggle(cx, y, "disable_p2p", config.disableP2p, v -> config.disableP2p = v);
        y += 8; drawHLine(y - 4); y += 6;

        // Network fields
        addRow2("rpc_host", config.rpcHost, cx, y); y += 18;
        addRow2("rpc_port", String.valueOf(config.rpcPort), cx, y); y += 18;
        addRow2("listen_url", config.listenUrl, cx, y); y += 18;
        addRow2("protocol", config.defaultProtocol, cx, y);
        y += 8; drawHLine(y - 4); y += 6;

        // More toggles
        addToggle(cx, y, "auto_start", config.autoStart, v -> config.autoStart = v); y += 17;
        addToggle(cx, y, "hud", config.hudEnabled, v -> config.hudEnabled = v); y += 24;

        maxScroll = Math.max(0, y + scrollY - (this.height - 40));
        if (scrollY > maxScroll) { scrollY = maxScroll; buildAdvanced(); }
    }

    // ============ CORE PAGE ============
    private void buildCore() {
        int cx = this.width / 2, y = 26;
        drawCentered(cx, y, t("easytier.core.title"), 0xFFAA00); y += 16;

        String v = NativeLoader.getCurrentVersion();
        drawCentered(cx, y, v, v.equals("not installed") ? 0xFF5555 : 0x55FF55); y += 12;
        drawCentered(cx, y, "Platform: " + NativeLoader.getPlatformId(), 0x888888); y += 18;

        drawHLine(y); y += 8;
        drawCentered(cx, y, "Place binaries in:", 0xAAAAAA); y += 12;
        drawCentered(cx, y, ".minecraft/easytier/bin/", 0xFFFFFF); y += 12;
        drawCentered(cx, y, "easytier-core + easytier-cli", 0xAAAAAA); y += 12;

        y += 4;
        addRenderableWidget(Button.builder(Component.literal("Open Folder"), b -> openFolder()).bounds(cx - 44, y, 88, 18).build());
    }

    // ============ WIDGET HELPERS ============
    private void addText(int x, int y, int color, String text) {
        addRenderableWidget(new MultiLineTextWidget(x, y, Component.literal(text).withColor(color), this.font));
    }
    private void drawCentered(int cx, int y, String text, int color) {
        int w = this.font.width(text.replaceAll("§[0-9a-fA-F]", ""));
        addRenderableWidget(new MultiLineTextWidget(cx - w / 2, y, Component.literal(text).withColor(color), this.font));
    }
    private void drawHLine(int y) {
        addRenderableWidget(new MultiLineTextWidget(8, y, Component.literal("§8" + "─".repeat((this.width - 16) / 4)), this.font));
    }

    private EditBox addRow(String key, String val, int cx, int y) {
        String label = t("easytier." + key);
        addText(cx - 124, y + 1, 0xAAAAAA, label);
        EditBox f = new EditBox(this.font, cx - 50, y, 160, 14, Component.empty());
        f.setValue(val != null ? val : "");
        f.setHint(Component.literal(t("easytier." + key + ".hint")));
        f.setMaxLength(256);
        addRenderableWidget(f);
        return f;
    }
    private void addRow2(String key, String val, int cx, int y) {
        String label = t("easytier." + key);
        addText(cx - 124, y + 1, 0xAAAAAA, label);
        EditBox f = new EditBox(this.font, cx - 50, y, 160, 14, Component.empty());
        f.setValue(val != null ? val : "");
        f.setMaxLength(256);
        addRenderableWidget(f);
    }
    private void addToggle(int cx, int y, String key, boolean val, java.util.function.Consumer<Boolean> s) {
        addRenderableWidget(CycleButton.onOffBuilder(val).create(cx - 55, y, 110, 14,
                Component.translatable("easytier." + key), (b, v) -> s.accept(v)));
    }
    private void bbtn(int x, int y, int w, String label, Runnable a) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> a.run()).bounds(x, y, w, 18).build());
    }

    // ============ ACTIONS ============
    private String t(String k) { return Component.translatable(k).getString(); }

    private void toggleProcess() {
        var p = EasyTierMod.getEasyTierProcess();
        if (p != null && p.isRunning()) EasyTierMod.stopEasyTier();
        else EasyTierMod.startEasyTier();
        build();
    }
    private void addConnector() {
        String u = peerUrlField != null ? peerUrlField.getValue().trim() : "";
        if (u.isEmpty()) return;
        EasyTierCli.execute(config, "connector", "add", u)
                .thenAccept(r -> { connectorList = "Added: " + u; build(); })
                .exceptionally(e -> { connectorList = "Error: " + e.getMessage(); build(); return null; });
    }
    private void refreshConnectors() {
        // Debounce: only refresh once per second
        long now = System.currentTimeMillis();
        if (now - connectorListFetch < 1000) return;
        connectorListFetch = now;
        connectorList = "...";
        EasyTierCli.execute(config, "connector", "list").thenAccept(r -> {
            connectorList = r.trim().isEmpty() ? "(none)" : r.trim().lines().count() + " peers";
            build();
        }).exceptionally(e -> { connectorList = "Error"; build(); return null; });
    }
    private void saveConfig() {
        if (hostnameField != null) config.hostname = hostnameField.getValue();
        if (networkNameField != null) config.networkName = networkNameField.getValue();
        if (networkSecretField != null) config.networkSecret = networkSecretField.getValue();
        config.save(FabricLoader.getInstance().getConfigDir());
    }
    private void openFolder() {
        try {
            String dir = NativeLoader.getBinDir().toAbsolutePath().toString();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("explorer", dir).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", dir).start();
            } else {
                new ProcessBuilder("xdg-open", dir).start();
            }
        } catch (Exception ex) {
            EasyTierMod.LOGGER.error("Cannot open folder: {}", ex.getMessage());
        }
    }

    private void saveAndClose() { saveConfig(); onClose(); }

    @Override public void onClose() { saveConfig(); if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(int k, int s, int m) {
        if (k == GLFW.GLFW_KEY_ESCAPE && parent == null) { onClose(); return true; }
        return super.keyPressed(k, s, m);
    }
    @Override public void render(GuiGraphics c, int mx, int my, float d) { super.render(c, mx, my, d); }
}
