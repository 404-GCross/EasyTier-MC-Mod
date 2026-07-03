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

/**
 * EasyTier config screen — all pages use framed scrollable content + fixed bottom buttons.
 */
public class EasyTierConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private enum Page { MAIN, ADVANCED, CORE }
    private Page page = Page.MAIN;
    private int scrollY, maxScroll;

    // Main fields
    private EditBox hostnameField, networkNameField, networkSecretField, peerUrlField;
    private String connectorList = "(none)";
    // Core fields
    private EditBox versionField;
    private String dlStatus = "";

    // Frame layout
    private int frameTop() { return 20; }
    private int frameBot() { return this.height - 42; }

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("easytier.title"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override protected void init() { page = Page.MAIN; scrollY = 0; buildWidgets(); }

    private void buildWidgets() {
        clearWidgets();
        switch (page) {
            case MAIN -> buildMain();
            case ADVANCED -> buildAdvanced();
            case CORE -> buildCore();
        }
    }

    // ============ MAIN ============
    private void buildMain() {
        int cx = this.width / 2;
        int frameH = frameBot() - frameTop();
        int y = frameTop() + 4 - scrollY;

        // Status
        String ver = NativeLoader.getCurrentVersion();
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        String status;
        if (!NativeLoader.isInstalled()) status = "§c" + t("easytier.status.not_installed");
        else if (running) status = "§a" + t("easytier.status.running") + " (v" + ver + ")";
        else status = "§7" + t("easytier.status.stopped") + " (v" + ver + ")";
        label(cx, y, status); y += 18;

        hostnameField = field(cx, y, 180, "easytier.hostname", "easytier.hostname.hint", config.hostname); y += 22;
        networkNameField = field(cx, y, 180, "easytier.network_name", "easytier.network_name.hint", config.networkName); y += 22;
        networkSecretField = field(cx, y, 180, "easytier.network_secret", "easytier.network_secret.hint", config.networkSecret); y += 22;
        peerUrlField = field(cx, y, 240, "easytier.peer_url", "easytier.peer_url.hint", ""); y += 1;
        btn(cx + 125, y, 20, "+", () -> addConnector());
        btn(cx + 148, y, 40, t("easytier.peers"), () -> refreshConnectors());
        y += 20;
        label(cx, y, "§7" + connectorList); y += 18;

        // Start/Stop button
        addRenderableWidget(Button.builder(
                Component.literal(running ? "§c" + t("easytier.stop") : "§a" + t("easytier.start")),
                b -> toggleProcess()
        ).bounds(cx - 55, y + 2, 110, 22).build());
        y += 30;

        y += scrollY;
        maxScroll = Math.max(0, y - (frameTop() + frameH));

        // Fixed bottom buttons
        int bw = 48, gap = 3, bx = this.width / 2 - (bw * 4 + gap * 3) / 2;
        int botY = frameBot() + 4;
        fixedBtn(bx, botY, bw, t("easytier.advanced"), () -> switchPage(Page.ADVANCED)); bx += bw + gap;
        fixedBtn(bx, botY, bw, t("easytier.core"), () -> switchPage(Page.CORE)); bx += bw + gap;
        fixedBtn(bx, botY, bw, t("easytier.save"), () -> saveAndClose()); bx += bw + gap;
        fixedBtn(bx, botY, bw, t("easytier.back"), () -> onClose());
    }

    // ============ ADVANCED ============
    private void buildAdvanced() {
        int cx = this.width / 2;
        int frameH = frameBot() - frameTop();
        int y = frameTop() + 4 - scrollY;

        title(y, "easytier.advanced.title"); y += 20;
        toggle(cx, y, "easytier.encryption", config.enableEncryption, v -> config.enableEncryption = v); y += 18;
        toggle(cx, y, "easytier.ipv6", config.enableIpv6, v -> config.enableIpv6 = v); y += 18;
        toggle(cx, y, "easytier.latency_first", config.latencyFirst, v -> config.latencyFirst = v); y += 18;
        toggle(cx, y, "easytier.kcp_proxy", config.enableKcpProxy, v -> config.enableKcpProxy = v); y += 18;
        toggle(cx, y, "easytier.quic_proxy", config.enableQuicProxy, v -> config.enableQuicProxy = v); y += 18;
        toggle(cx, y, "easytier.disable_p2p", config.disableP2p, v -> config.disableP2p = v); y += 20;
        field(cx, y, 180, "easytier.rpc_host", null, config.rpcHost); y += 20;
        field(cx, y, 60, "easytier.rpc_port", null, String.valueOf(config.rpcPort)); y += 20;
        field(cx, y, 240, "easytier.listen_url", null, config.listenUrl); y += 20;
        field(cx, y, 60, "easytier.protocol", null, config.defaultProtocol); y += 20;
        toggle(cx, y, "easytier.auto_start", config.autoStart, v -> config.autoStart = v); y += 18;
        toggle(cx, y, "easytier.hud", config.hudEnabled, v -> config.hudEnabled = v); y += 20;
        field(cx, y, 240, "easytier.api_mirror", null, config.apiMirror); y += 20;
        field(cx, y, 240, "easytier.dl_mirror", null, config.downloadMirror); y += 24;

        y += scrollY;
        maxScroll = Math.max(0, y - (frameTop() + frameH));

        int botY = frameBot() + 4;
        fixedBtn(this.width / 2 - 86, botY, 80, t("easytier.back"), () -> switchPage(Page.MAIN));
        fixedBtn(this.width / 2 + 6, botY, 80, t("easytier.save"), () -> switchPage(Page.MAIN));
    }

    // ============ CORE ============
    private void buildCore() {
        int cx = this.width / 2;
        int frameH = frameBot() - frameTop();
        int y = frameTop() + 4 - scrollY;

        title(y, "easytier.core.title"); y += 20;
        label(cx, y, "§e" + t("easytier.core.current") + NativeLoader.getCurrentVersion()); y += 18;

        versionField = new EditBox(this.font, cx - 70, y, 110, 20, Component.empty());
        versionField.setHint(Component.translatable("easytier.core.version_hint"));
        addRenderableWidget(versionField);
        btn(cx + 45, y, 50, t("easytier.core.versions"), () -> fetchVersions());
        btn(cx + 100, y, 50, t("easytier.core.download"), () -> downloadVersion());
        y += 24;
        label(cx, y, "§7" + dlStatus); y += 22;
        field(cx, y, 240, "easytier.dl_mirror", null, config.downloadMirror); y += 24;

        y += scrollY;
        maxScroll = Math.max(0, y - (frameTop() + frameH));

        int botY = frameBot() + 4;
        fixedBtn(this.width / 2 - 40, botY, 80, t("easytier.back"), () -> switchPage(Page.MAIN));
    }

    // ============ COMMON WIDGETS ============
    private void switchPage(Page p) { saveConfig(); page = p; scrollY = 0; buildWidgets(); }

    private void title(int y, String key) {
        addRenderableWidget(new MultiLineTextWidget(this.width / 2 - 50, y,
                Component.translatable(key).withColor(0xFFAA00), this.font));
    }

    private void label(int cx, int y, String text) {
        addRenderableWidget(new MultiLineTextWidget(cx - text.length() * 3, y,
                Component.literal(text), this.font));
    }

    private EditBox field(int cx, int y, int w, String labelKey, String hintKey, String value) {
        if (labelKey != null)
            addRenderableWidget(new MultiLineTextWidget(cx - w / 2 - 80, y,
                    Component.translatable(labelKey), this.font));
        EditBox f = new EditBox(this.font, cx - w / 2, y, w, 16, Component.empty());
        f.setValue(value != null ? value : "");
        if (hintKey != null) f.setHint(Component.translatable(hintKey));
        f.setMaxLength(256);
        addRenderableWidget(f);
        return f;
    }

    private void toggle(int cx, int y, String key, boolean val, java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(CycleButton.onOffBuilder(val).create(cx - 80, y, 160, 16,
                Component.translatable(key), (b, v) -> setter.accept(v)));
    }

    private void btn(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, 16).build());
    }

    private void fixedBtn(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, 18).build());
    }

    // ============ SCROLL + RENDER ============
    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollY = (int) Math.clamp(scrollY - v * 20, 0, maxScroll);
        buildWidgets();
        return true;
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        // Frame around content
        int fx = 3, fy = frameTop(), fw = this.width - 6, fh = frameBot() - frameTop();
        ctx.fill(fx, fy, fx + fw, fy + fh, 0x15FFFFFF);
        ctx.renderOutline(fx, fy, fw, fh, 0x88AAAAAA);
        // Scrollbar
        if (maxScroll > 0) {
            int barH = Math.max(12, (int)((float)fh / (fh + maxScroll) * fh));
            int barY = fy + (int)((float)scrollY / maxScroll * (fh - barH));
            ctx.fill(fx + fw - 3, barY, fx + fw, barY + barH, 0x55FFFFFF);
        }
    }

    // ============ ACTIONS ============
    private String t(String key) { return Component.translatable(key).getString(); }

    private void toggleProcess() {
        var proc = EasyTierMod.getEasyTierProcess();
        if (proc != null && proc.isRunning()) EasyTierMod.stopEasyTier();
        else EasyTierMod.startEasyTier();
        buildWidgets();
    }

    private void addConnector() {
        String url = peerUrlField != null ? peerUrlField.getValue().trim() : "";
        if (url.isEmpty()) return;
        EasyTierCli.execute(config, "connector", "add", url)
                .thenAccept(r -> { connectorList = "Added"; buildWidgets(); })
                .exceptionally(e -> { connectorList = "Error"; buildWidgets(); return null; });
    }

    private void refreshConnectors() {
        connectorList = "...";
        EasyTierCli.execute(config, "connector", "list").thenAccept(r -> {
            connectorList = r.trim().isEmpty() ? t("easytier.peers.none")
                    : r.trim().lines().count() + " peers";
            buildWidgets();
        }).exceptionally(e -> { connectorList = "Error"; buildWidgets(); return null; });
    }

    private void fetchVersions() {
        dlStatus = t("easytier.core.fetching");
        NativeLoader.fetchAvailableVersions().thenAccept(vs -> {
            if (vs.isEmpty()) { dlStatus = t("easytier.core.no_versions"); buildWidgets(); return; }
            versionField.setValue(vs.get(0).tag);
            var sb = new StringBuilder();
            for (int i = 0; i < Math.min(10, vs.size()); i++) sb.append(vs.get(i).tag).append("  ");
            dlStatus = sb.toString();
            buildWidgets();
        }).exceptionally(e -> { dlStatus = "Error"; buildWidgets(); return null; });
    }

    private void downloadVersion() {
        String v = versionField != null ? versionField.getValue().trim() : "";
        if (v.isEmpty()) { dlStatus = t("easytier.core.enter_version"); buildWidgets(); return; }
        dlStatus = "Downloading " + v + "...";
        buildWidgets();
        NativeLoader.downloadUpdate(v, msg -> { dlStatus = msg; buildWidgets(); });
    }

    private void saveConfig() {
        if (hostnameField != null) config.hostname = hostnameField.getValue();
        if (networkNameField != null) config.networkName = networkNameField.getValue();
        if (networkSecretField != null) config.networkSecret = networkSecretField.getValue();
        config.save(FabricLoader.getInstance().getConfigDir());
    }

    private void saveAndClose() { saveConfig(); onClose(); }

    @Override public void onClose() {
        saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
    @Override public boolean isPauseScreen() { return false; }
}
