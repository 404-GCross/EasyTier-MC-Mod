package com.easytier.mcmod.screen;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.config.ModConfig;
import com.easytier.mcmod.easytier.NativeLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
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

    private EditBox hostnameField, networkNameField, networkSecretField, peersField, ipv4Field;

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("easytier.title"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    // Re-check state every time init is called (window switch etc.)
    @Override protected void init() { page = 0; scrollY = 0; build(); }

    private void build() {
        clearWidgets();
        if (page == 0) buildMain();
        else if (page == 1) buildAdvanced();
        else if (page == 2) buildCore();
        else buildLogs();
        bottomBar();
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (page == 1 || page == 3) {
            scrollY = (int) Math.clamp(scrollY - v * 20, 0, Math.max(0, maxScroll));
            build();
            return true;
        }
        return false;
    }

    // ============ BOTTOM BAR ============
    private void bottomBar() {
        int bot = this.height - 24, cx = this.width / 2;
        if (page == 0) {
            int[] ws = {52, 46, 38, 52, 46};
            String[] ls = {t("easytier.advanced"), t("easytier.core"), t("easytier.logs"), t("easytier.save"), "✕"};
            Runnable[] as = {()->{page=1;build();}, ()->{page=2;build();}, ()->{page=3;build();}, this::saveAndClose, this::onClose};
            layoutButtons(cx, bot, ws, ls, as);
        } else {
            int[] ws = {56, 56, 46, 46};
            String[] ls = {t("easytier.back"), t("easytier.save"), "", "✕"};
            Runnable[] as = {()->{saveConfig();page=0;scrollY=0;build();}, this::saveAndClose, null, this::onClose};
            layoutButtons(cx, bot, ws, ls, as);
        }
    }

    private void layoutButtons(int cx, int y, int[] ws, String[] ls, Runnable[] as) {
        int total = 0;
        for (int w : ws) total += w;
        int gap = 3, totalW = total + gap * (ws.length - 1);
        int bx = cx - totalW / 2;
        for (int i = 0; i < ws.length; i++) {
            if (as[i] == null) { bx += ws[i] + gap; continue; }
            final Runnable action = as[i];
            final int w = ws[i];
            addRenderableWidget(Button.builder(Component.literal(ls[i]), b -> action.run())
                    .bounds(bx, y, w, 18).build());
            bx += w + gap;
        }
    }

    // ============ MAIN PAGE ============
    private void buildMain() {
        int cx = this.width / 2, y = 26;
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        String ver = NativeLoader.getCurrentVersion();

        int sc = !NativeLoader.isInstalled() ? 0xFF5555 : running ? 0x55FF55 : 0xAAAAAA;
        drawCentered(cx, y, (running ? "● " : "○ ") + (running ? t("easytier.status.running") : NativeLoader.isInstalled() ? t("easytier.status.stopped") : t("easytier.status.not_installed")), sc);
        y += 12;
        drawCentered(cx, y, "§8v" + ver, 0x888888);
        y += 18;
        drawHLine(y); y += 6;

        hostnameField = addRow("hostname", config.hostname, cx, y); y += 20;
        networkNameField = addRow("networkName", config.networkName, cx, y); y += 20;
        networkSecretField = addRow("networkSecret", config.networkSecret, cx, y); y += 20;
        peersField = addRow("peerUrl", config.peers, cx, y); y += 20;

        // IP/DHCP
        addText(cx - 150, y + 1, 0xAAAAAA, config.dhcp ? "DHCP" : "IPv4");
        if (!config.dhcp) {
            ipv4Field = new EditBox(this.font, cx - 40, y, 100, 14, Component.empty());
            ipv4Field.setValue(config.ipv4); ipv4Field.setHint(Component.literal("10.1.1.1"));
            ipv4Field.setMaxLength(15); addRenderableWidget(ipv4Field);
        }
        addRenderableWidget(Button.builder(Component.literal(config.dhcp ? t("easytier.ip_fix") : t("easytier.ip_auto")),
                b -> { config.dhcp = !config.dhcp; saveConfig(); build(); }).bounds(cx + 65, y, 40, 14).build());
        y += 22;

        addRenderableWidget(Button.builder(
                Component.literal(running ? "■ " + t("easytier.stop") : "▶ " + t("easytier.start")),
                b -> toggleProcess()).bounds(cx - 55, y, 110, 22).build());
        y += 26;

        // Log line
        if (proc != null) {
            var logs = proc.getRecentLogs(1);
            if (!logs.isEmpty()) {
                String last = logs.getFirst();
                if (last.length() > 65) last = last.substring(0, 62) + "...";
                drawCentered(cx, y, "§8" + last, 0x666666);
            }
        }
        if (!running && NativeLoader.isInstalled()) {
            drawCentered(cx, y, "§7Click Start to launch", 0x666666);
        }
        if (!NativeLoader.isInstalled()) {
            drawCentered(cx, y, "§cGo to Core page to install binaries", 0xFF6666);
        }
    }

    // ============ ADVANCED PAGE ============
    private void buildAdvanced() {
        int cx = this.width / 2, y = 26 - scrollY, gap = 20;
        drawCentered(cx, y, t("easytier.advanced.title"), 0xFFAA00); y += gap;

        // Toggles
        addToggle(cx, y, "encryption", config.enableEncryption, v -> config.enableEncryption = v); y += gap;
        addToggle(cx, y, "ipv6", config.enableIpv6, v -> config.enableIpv6 = v); y += gap;
        addToggle(cx, y, "latency_first", config.latencyFirst, v -> config.latencyFirst = v); y += gap;
        addToggle(cx, y, "kcp_proxy", config.enableKcpProxy, v -> config.enableKcpProxy = v); y += gap;
        addToggle(cx, y, "quic_proxy", config.enableQuicProxy, v -> config.enableQuicProxy = v); y += gap;
        addToggle(cx, y, "disable_p2p", config.disableP2p, v -> config.disableP2p = v);
        y += 8; drawHLine(y-4); y += 6;

        addRow2("rpc_host", config.rpcHost, cx, y); y += gap;
        addRow2("rpc_port", String.valueOf(config.rpcPort), cx, y); y += gap;
        addRow2("listen_url", config.listenUrl, cx, y); y += gap;
        addRow2("protocol", config.defaultProtocol, cx, y);
        y += 8; drawHLine(y-4); y += 6;

        addToggle(cx, y, "auto_start", config.autoStart, v -> config.autoStart = v); y += gap;
        addToggle(cx, y, "hud", config.hudEnabled, v -> config.hudEnabled = v); y += gap + 10;

        maxScroll = Math.max(0, y + scrollY - (this.height - 50));
    }

    // ============ CORE PAGE ============
    private void buildCore() {
        int cx = this.width / 2, y = 26;
        String plat = NativeLoader.getPlatformId();
        boolean win = plat.startsWith("windows");
        String ext = win ? ".exe" : "";

        drawCentered(cx, y, t("easytier.core.title"), 0xFFAA00); y += 16;
        String v = NativeLoader.getCurrentVersion();
        drawCentered(cx, y, v, v.equals("not installed") ? 0xFF5555 : 0x55FF55); y += 14;
        drawHLine(y); y += 8;

        drawCentered(cx, y, "Download: github.com/EasyTier/EasyTier/releases", 0xFFFFFF); y += 16;
        addRenderableWidget(Button.builder(Component.literal(t("easytier.open_browser")), b ->
                Util.getPlatform().openUri("https://github.com/EasyTier/EasyTier/releases"))
                .bounds(cx - 50, y, 100, 16).build()); y += 20;
        drawCentered(cx, y, "Platform: " + plat, 0x888888); y += 14;
        drawHLine(y); y += 8;

        String core = "easytier-core-" + plat + ext;
        String cli  = "easytier-cli-" + plat + ext;
        drawCentered(cx, y, "Rename & place in .minecraft/easytier/bin/:", 0xAAAAAA); y += 12;
        drawCentered(cx, y, core + " → easytier-core" + ext, 0x55FF55); y += 12;
        drawCentered(cx, y, cli + " → easytier-cli" + ext, 0x55FF55); y += 16;

        addRenderableWidget(Button.builder(Component.literal(t("easytier.open_folder")), b -> openFolder())
                .bounds(cx - 80, y, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Test Binary"), b -> {
            testResult = NativeLoader.testBinary();
            build();
        }).bounds(cx, y, 70, 18).build());
        if (testResult != null) {
            y += 22;
            String[] lines = testResult.split("\n");
            for (String line : lines) {
                drawCentered(cx, y, "§e" + line, 0xFFFF55); y += 10;
            }
        }
    }

    private String testResult = null;

    // ============ LOGS PAGE ============
    private void buildLogs() {
        int cx = this.width / 2, y = 26;
        drawCentered(cx, y, t("easytier.logs.title"), 0xFFAA00); y += 18;

        var proc = EasyTierMod.getEasyTierProcess();
        if (proc == null || !proc.isRunning()) {
            drawCentered(cx, y + 10, t("easytier.logs.not_running"), 0x888888);
            return;
        }

        var logs = proc.getRecentLogs(500);
        y -= scrollY;
        for (String line : logs) {
            if (line.length() > 70) line = line.substring(0, 67) + "...";
            addText(4, y, 0xAAAAAA, line);
            y += 10;
        }
        maxScroll = Math.max(0, y + scrollY - (this.height - 50));
    }

    // ============ WIDGETS ============
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
        addText(cx - 150, y + 1, 0xAAAAAA, t("easytier." + key));
        EditBox f = new EditBox(this.font, cx - 40, y, 150, 14, Component.empty());
        f.setValue(val != null ? val : "");
        f.setHint(Component.literal(t("easytier." + key + ".hint")));
        f.setMaxLength(256); addRenderableWidget(f);
        return f;
    }
    private void addRow2(String key, String val, int cx, int y) {
        addText(cx - 150, y + 1, 0xAAAAAA, t("easytier." + key));
        EditBox f = new EditBox(this.font, cx - 40, y, 150, 14, Component.empty());
        f.setValue(val != null ? val : ""); f.setMaxLength(256); addRenderableWidget(f);
    }
    private void addToggle(int cx, int y, String key, boolean val, java.util.function.Consumer<Boolean> s) {
        addRenderableWidget(CycleButton.onOffBuilder(val).create(cx - 55, y, 105, 14,
                Component.translatable("easytier." + key), (b, v) -> s.accept(v)));
    }

    // ============ ACTIONS ============
    private String t(String k) { return Component.translatable(k).getString(); }

    private void toggleProcess() {
        saveConfig();
        var p = EasyTierMod.getEasyTierProcess();
        if (p != null && p.isRunning()) {
            build();
            new Thread(() -> { EasyTierMod.stopEasyTier(); build(); }, "ET-stop").start();
        } else {
            EasyTierMod.startEasyTier();
            build();
        }
    }
    private void saveConfig() {
        if (hostnameField != null) config.hostname = hostnameField.getValue();
        if (networkNameField != null) config.networkName = networkNameField.getValue();
        if (networkSecretField != null) config.networkSecret = networkSecretField.getValue();
        if (peersField != null) config.peers = peersField.getValue();
        if (ipv4Field != null) config.ipv4 = ipv4Field.getValue();
        config.save(FabricLoader.getInstance().getConfigDir());
    }
    private void openFolder() {
        try {
            String dir = NativeLoader.getBinDir().toAbsolutePath().toString();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("explorer", dir).start();
            else if (os.contains("mac")) new ProcessBuilder("open", dir).start();
            else new ProcessBuilder("xdg-open", dir).start();
        } catch (Exception ex) { EasyTierMod.LOGGER.error("Cannot open folder: {}", ex.getMessage()); }
    }
    private void saveAndClose() { saveConfig(); onClose(); }

    @Override public void onClose() { saveConfig(); if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(int k, int s, int m) {
        if (k == GLFW.GLFW_KEY_ESCAPE && parent == null) { onClose(); return true; }
        return super.keyPressed(k, s, m);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float d) { super.render(g, mx, my, d); }
}
