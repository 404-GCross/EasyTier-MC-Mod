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
    private int page; // 0=main, 1=advanced, 2=core

    private EditBox hostnameField, networkNameField, networkSecretField, peerUrlField;
    private String connectorList = "";
    private int scrollY, maxScroll;

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("easytier.title"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override protected void init() { page = 0; scrollY = 0; build(); }

    private void build() {
        clearWidgets();
        switch (page) {
            case 0 -> buildMain();
            case 1 -> buildAdvanced();
            case 2 -> buildCore();
        }
        bottomBar();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (page == 1) { // only advanced page scrolls
            scrollY = (int) Math.clamp(scrollY - v * 20, 0, maxScroll);
            build();
            return true;
        }
        return false;
    }

    private int sY() { return page == 1 ? scrollY : 0; }

    private void bottomBar() {
        int bot = this.height - 24;
        int bw = 46, gap = 3, cx = this.width / 2;
        int bx = cx - (bw * 4 + gap * 3) / 2;
        addRenderableWidget(Button.builder(Component.literal(page != 0 ? t("easytier.back") : t("easytier.advanced")),
                b -> { if (page != 0) { saveConfig(); page = 0; scrollY = 0; } else page = 1; build(); })
                .bounds(bx, bot, bw, 18).build()); bx += bw + gap;
        addRenderableWidget(Button.builder(Component.literal(page != 0 ? t("easytier.save") : t("easytier.core")),
                b -> { if (page != 0) { saveConfig(); page = 0; scrollY = 0; build(); } else page = 2; build(); })
                .bounds(bx, bot, bw, 18).build()); bx += bw + gap;
        addRenderableWidget(Button.builder(Component.literal(t("easytier.save")),
                b -> saveAndClose()).bounds(bx, bot, bw, 18).build()); bx += bw + gap;
        addRenderableWidget(Button.builder(Component.literal("✔"),
                b -> onClose()).bounds(bx, bot, bw, 18).build());
    }

    // ---- MAIN ----
    private void buildMain() {
        int cx = this.width / 2, gap = 5, y = 32;
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        String ver = NativeLoader.getCurrentVersion();

        String st = !NativeLoader.isInstalled() ? "§c" + t("easytier.status.not_installed")
                : running ? "§a" + t("easytier.status.running") + "  v" + ver
                : "§7" + t("easytier.status.stopped") + "  v" + ver;
        addText(cx - 90, y, st); y += 18;

        hostnameField = addField("hostname", config.hostname, cx, y); y += 18;
        networkNameField = addField("networkName", config.networkName, cx, y); y += 18;
        networkSecretField = addField("networkSecret", config.networkSecret, cx, y); y += 18;
        peerUrlField = addField("peerUrl", "", cx, y); y += 1;

        int x2 = cx + 115;
        addBtn(x2, y, 18, "+", () -> addConnector());
        addBtn(x2 + 24, y, 36, t("easytier.peers"), () -> refreshConnectors());
        y += 20;
        addText(cx - 100, y, "§7" + connectorList); y += 14;

        addRenderableWidget(Button.builder(
                Component.literal(running ? "§c" + t("easytier.stop") : "§a" + t("easytier.start")),
                b -> toggleProcess()).bounds(cx - 55, y, 110, 20).build());
    }

    // ---- ADVANCED ----
    private void buildAdvanced() {
        int cx = this.width / 2, y = 27 - scrollY;
        addText(cx - 40, y, "§6" + t("easytier.advanced.title")); y += 16;
        addToggle("encryption", config.enableEncryption, v -> config.enableEncryption = v, cx, y); y += 16;
        addToggle("ipv6", config.enableIpv6, v -> config.enableIpv6 = v, cx, y); y += 16;
        addToggle("latency_first", config.latencyFirst, v -> config.latencyFirst = v, cx, y); y += 16;
        addToggle("kcp_proxy", config.enableKcpProxy, v -> config.enableKcpProxy = v, cx, y); y += 16;
        addToggle("quic_proxy", config.enableQuicProxy, v -> config.enableQuicProxy = v, cx, y); y += 16;
        addToggle("disable_p2p", config.disableP2p, v -> config.disableP2p = v, cx, y); y += 18;
        addField2("rpc_host", config.rpcHost, cx, y); y += 18;
        addField2("rpc_port", String.valueOf(config.rpcPort), cx, y); y += 18;
        addField2("listen_url", config.listenUrl, cx, y); y += 18;
        addField2("protocol", config.defaultProtocol, cx, y); y += 18;
        addToggle("auto_start", config.autoStart, v -> config.autoStart = v, cx, y); y += 16;
        addToggle("hud", config.hudEnabled, v -> config.hudEnabled = v, cx, y); y += 24;

        maxScroll = Math.max(0, y + scrollY - (this.height - 40));
        if (scrollY > maxScroll) { scrollY = maxScroll; buildAdvanced(); }
    }

    // ---- CORE ----
    private void buildCore() {
        int cx = this.width / 2, y = 30;
        addText(cx - 40, y, "§6" + t("easytier.core.title")); y += 20;
        addText(cx - 100, y, "§e" + t("easytier.core.current") + NativeLoader.getCurrentVersion()); y += 16;
        addText(cx - 100, y, "§7Platform: " + NativeLoader.getPlatformId()); y += 16;
        addText(cx - 100, y, "§7Place binaries in:"); y += 14;
        addText(cx - 100, y, "§7  .minecraft/easytier/bin/"); y += 14;
        addText(cx - 100, y, "§7  easytier-core + easytier-cli"); y += 14;
        addBtn(cx - 40, y, 80, "Open Folder", () -> {
            try { java.awt.Desktop.getDesktop().open(NativeLoader.getBinDir().toFile()); }
            catch (Exception ex) { EasyTierMod.LOGGER.error("[EasyTier] Cannot open folder: {}", ex.getMessage()); }
        });
    }

    // ---- Widget shortcuts ----
    private void addText(int x, int y, String text) {
        addRenderableWidget(new MultiLineTextWidget(x, y, Component.literal(text), this.font));
    }

    private EditBox addField(String key, String val, int cx, int y) {
        int x = cx - 100;
        String label = t("easytier." + key);
        String hint = t("easytier." + key + ".hint");
        addText(x - 80, y, label);
        EditBox f = new EditBox(this.font, x + 60, y, 140, 14, Component.empty());
        f.setValue(val != null ? val : "");
        f.setHint(Component.literal(hint));
        f.setMaxLength(256);
        addRenderableWidget(f);
        return f;
    }

    private void addField2(String key, String val, int cx, int y) {
        String label = t("easytier." + key);
        addText(cx - 140, y, label);
        EditBox f = new EditBox(this.font, cx - 20, y, 120, 14, Component.empty());
        f.setValue(val != null ? val : "");
        f.setMaxLength(256);
        addRenderableWidget(f);
    }

    private void addToggle(String key, boolean val, java.util.function.Consumer<Boolean> s, int cx, int y) {
        addRenderableWidget(CycleButton.onOffBuilder(val).create(cx - 60, y, 120, 14,
                Component.translatable("easytier." + key), (b, v) -> s.accept(v)));
    }

    private void addBtn(int x, int y, int w, String label, Runnable a) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> a.run()).bounds(x, y, w, 14).build());
    }

    // ---- Actions ----
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
                .thenAccept(r -> { connectorList = u; build(); });
    }
    private void refreshConnectors() {
        EasyTierCli.execute(config, "connector", "list").thenAccept(r -> {
            connectorList = r.trim().isEmpty() ? "(none)" : r.trim().lines().count() + " peers";
            build();
        });
    }
    private void saveConfig() {
        if (hostnameField != null) config.hostname = hostnameField.getValue();
        if (networkNameField != null) config.networkName = networkNameField.getValue();
        if (networkSecretField != null) config.networkSecret = networkSecretField.getValue();
        config.save(FabricLoader.getInstance().getConfigDir());
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
