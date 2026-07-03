package com.easytier.mcmod.screen;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.config.ModConfig;
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

    private EditBox hostnameField, networkNameField, networkSecretField, peersField, ipv4Field;

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
        peersField = addRow("peerUrl", config.peers, cx, y); y += 20;

        // IP with DHCP toggle
        String ipLabel = config.dhcp ? "DHCP" : "IPv4";
        addText(cx - 150, y + 1, 0xAAAAAA, ipLabel);
        if (!config.dhcp) {
            ipv4Field = new EditBox(this.font, cx - 40, y, 100, 14, Component.empty());
            ipv4Field.setValue(config.ipv4);
            ipv4Field.setHint(Component.literal("10.1.1.1"));
            ipv4Field.setMaxLength(15);
            addRenderableWidget(ipv4Field);
        }
        addRenderableWidget(Button.builder(Component.literal(config.dhcp ? "Fix IP" : "Auto"),
                b -> { config.dhcp = !config.dhcp; saveConfig(); build(); })
                .bounds(cx + 65, y, 40, 14).build());
        y += 20;

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
        String plat = NativeLoader.getPlatformId();
        boolean win = plat.startsWith("windows");
        String ext = win ? ".exe" : "";

        drawCentered(cx, y, t("easytier.core.title"), 0xFFAA00); y += 16;

        String v = NativeLoader.getCurrentVersion();
        drawCentered(cx, y, v, v.equals("not installed") ? 0xFF5555 : 0x55FF55); y += 14;

        drawHLine(y); y += 8;
        drawCentered(cx, y, "Download from GitHub Releases:", 0xAAAAAA); y += 12;
        drawCentered(cx, y, "github.com/EasyTier/EasyTier/releases", 0xFFFFFF); y += 12;
        drawCentered(cx, y, "Your platform: " + plat, 0x888888); y += 14;

        drawHLine(y); y += 8;
        drawCentered(cx, y, "Rename & place in .minecraft/easytier/bin/:", 0xAAAAAA); y += 12;
        String core = "easytier-core-" + plat + ext;
        String cli  = "easytier-cli-" + plat + ext;
        drawCentered(cx, y, core + " → easytier-core" + ext, 0x55FF55); y += 12;
        drawCentered(cx, y, cli + " → easytier-cli" + ext, 0x55FF55); y += 14;

        y += 2;
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
        addText(cx - 150, y + 1, 0xAAAAAA, label);
        EditBox f = new EditBox(this.font, cx - 40, y, 150, 14, Component.empty());
        f.setValue(val != null ? val : "");
        f.setHint(Component.literal(t("easytier." + key + ".hint")));
        f.setMaxLength(256);
        addRenderableWidget(f);
        return f;
    }
    private void addRow2(String key, String val, int cx, int y) {
        String label = t("easytier." + key);
        addText(cx - 150, y + 1, 0xAAAAAA, label);
        EditBox f = new EditBox(this.font, cx - 40, y, 150, 14, Component.empty());
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
        saveConfig(); // save all fields before starting
        var p = EasyTierMod.getEasyTierProcess();
        if (p != null && p.isRunning()) {
            build(); // immediately show "stopping" state
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
