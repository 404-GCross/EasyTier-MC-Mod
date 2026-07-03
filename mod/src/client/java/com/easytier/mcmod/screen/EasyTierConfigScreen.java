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
 * EasyTier config screen — simple main page, advanced on second level.
 */
public class EasyTierConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    private boolean showingAdvanced;

    // Main page fields
    private EditBox hostnameField, networkNameField, networkSecretField, peerUrlField;
    private String connectorList = "(none)";
    private String statusMsg = "";

    // Core page fields
    private EditBox versionField;
    private String dlStatus = "";

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("easytier.title"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override
    protected void init() { showingAdvanced = false; buildMain(); }

    // ============ MAIN ============
    private void buildMain() {
        clearWidgets();
        int cx = this.width / 2;
        int y = 28;

        // Status
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        String ver = NativeLoader.getCurrentVersion();
        if (!NativeLoader.isInstalled()) statusMsg = t("easytier.status.not_installed");
        else if (running) statusMsg = "§a" + t("easytier.status.running") + " §7(v" + ver + ")";
        else statusMsg = "§7" + t("easytier.status.stopped") + " (v" + ver + ")";

        addRenderableWidget(new MultiLineTextWidget(cx - 100, y,
                Component.literal(statusMsg), this.font));
        y += 20;

        // Fields
        hostnameField = field(cx, y, 180, "easytier.hostname", "easytier.hostname.hint", config.hostname); y += 22;
        networkNameField = field(cx, y, 180, "easytier.network_name", "easytier.network_name.hint", config.networkName); y += 22;
        networkSecretField = field(cx, y, 180, "easytier.network_secret", "easytier.network_secret.hint", config.networkSecret); y += 22;

        // Peer URL + Add/List
        peerUrlField = field(cx, y, 240, "easytier.peer_url", "easytier.peer_url.hint", ""); y += 1;
        btn(cx + 125, y, 20, "+", () -> addConnector());
        btn(cx + 148, y, 40, t("easytier.peers"), () -> refreshConnectors());
        y += 20;
        addRenderableWidget(new MultiLineTextWidget(cx - 120, y,
                Component.literal("§7" + connectorList), this.font));
        y += 18;

        // Bottom bar
        int botY = this.height - 28;

        // Start/Stop button
        addRenderableWidget(Button.builder(
                Component.literal(running ? "§c" + t("easytier.stop") : "§a" + t("easytier.start")),
                btn -> toggleProcess()
        ).bounds(this.width / 2 - 55, botY - 10, 110, 20).build());

        // Four small buttons
        btn(this.width / 2 - 128, botY + 12, 60, t("easytier.advanced"), () -> buildAdvanced());
        btn(this.width / 2 - 64, botY + 12, 40, t("easytier.core"), () -> buildCorePage());
        btn(this.width / 2 - 20, botY + 12, 40, t("easytier.save"), () -> saveAndClose());
        btn(this.width / 2 + 24, botY + 12, 40, t("easytier.back"), () -> onClose());
    }

    // ============ ADVANCED ============
    private void buildAdvanced() {
        showingAdvanced = true;
        clearWidgets();
        int cx = this.width / 2;
        int y = 22;
        addTitle(y, "easytier.advanced.title"); y += 20;

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

        // Bottom buttons
        int botY = this.height - 28;
        btn(this.width / 2 - 80, botY, 70, t("easytier.back"), () -> { saveConfig(); buildMain(); });
        btn(this.width / 2 + 10, botY, 70, t("easytier.save"), () -> { saveConfig(); buildMain(); });
    }

    // ============ CORE ============
    private void buildCorePage() {
        showingAdvanced = true;
        clearWidgets();
        int cx = this.width / 2;
        int y = 22;
        addTitle(y, "easytier.core.title"); y += 20;

        addRenderableWidget(new MultiLineTextWidget(cx - 100, y,
                Component.literal("§e" + t("easytier.core.current") + NativeLoader.getCurrentVersion()), this.font));
        y += 18;

        versionField = new EditBox(this.font, cx - 70, y, 110, 20, Component.empty());
        versionField.setHint(Component.translatable("easytier.core.version_hint"));
        addRenderableWidget(versionField);
        btn(cx + 45, y, 50, t("easytier.core.versions"), () -> fetchVersions());
        btn(cx + 100, y, 50, t("easytier.core.download"), () -> downloadVersion());
        y += 24;

        addRenderableWidget(new MultiLineTextWidget(cx - 110, y,
                Component.literal("§7" + dlStatus), this.font));
        y += 22;

        field(cx, y, 240, "easytier.dl_mirror", null, config.downloadMirror); y += 24;

        int botY = this.height - 28;
        btn(this.width / 2 - 35, botY, 70, t("easytier.back"), () -> { saveConfig(); buildMain(); });
    }

    // ============ HELPERS ============
    private String t(String key) {
        return Component.translatable(key).getString();
    }

    private void addTitle(int y, String key) {
        addRenderableWidget(new MultiLineTextWidget(this.width / 2 - 50, y,
                Component.translatable(key).withColor(0xFFAA00), this.font));
    }

    private EditBox field(int cx, int y, int w, String labelKey, String hintKey, String value) {
        if (labelKey != null) {
            addRenderableWidget(new MultiLineTextWidget(cx - w / 2 - 80, y,
                    Component.translatable(labelKey), this.font));
        }
        EditBox f = new EditBox(this.font, cx - w / 2, y, w, 16, Component.empty());
        f.setValue(value != null ? value : "");
        if (hintKey != null) f.setHint(Component.translatable(hintKey));
        f.setMaxLength(256);
        addRenderableWidget(f);
        return f;
    }

    private void toggle(int cx, int y, String key, boolean val, java.util.function.Consumer<Boolean> setter) {
        var btn = CycleButton.onOffBuilder(val).create(cx - 80, y, 160, 16,
                Component.translatable(key), (b, v) -> setter.accept(v));
        addRenderableWidget(btn);
    }

    private void btn(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, 16).build());
    }

    // ============ ACTIONS ============
    private void toggleProcess() {
        var proc = EasyTierMod.getEasyTierProcess();
        if (proc != null && proc.isRunning()) EasyTierMod.stopEasyTier();
        else EasyTierMod.startEasyTier();
        buildMain();
    }

    private void addConnector() {
        String url = peerUrlField != null ? peerUrlField.getValue().trim() : "";
        if (url.isEmpty()) { connectorList = t("easytier.peer_url.hint"); buildMain(); return; }
        EasyTierCli.execute(config, "connector", "add", url)
                .thenAccept(r -> { connectorList = "Added"; buildMain(); })
                .exceptionally(e -> { connectorList = "Error"; buildMain(); return null; });
    }

    private void refreshConnectors() {
        connectorList = "...";
        EasyTierCli.execute(config, "connector", "list").thenAccept(r -> {
            connectorList = r.trim().isEmpty() ? t("easytier.peers.none") : r.trim().lines().count() + " peers";
            buildMain();
        }).exceptionally(e -> { connectorList = "Error"; buildMain(); return null; });
    }

    private void fetchVersions() {
        dlStatus = t("easytier.core.fetching");
        NativeLoader.fetchAvailableVersions().thenAccept(vs -> {
            if (vs.isEmpty()) { dlStatus = t("easytier.core.no_versions"); buildCorePage(); return; }
            versionField.setValue(vs.get(0).tag);
            var sb = new StringBuilder();
            for (int i = 0; i < Math.min(10, vs.size()); i++)
                sb.append(vs.get(i).tag).append("  ");
            dlStatus = sb.toString();
            buildCorePage();
        }).exceptionally(e -> { dlStatus = "Error"; buildCorePage(); return null; });
    }

    private void downloadVersion() {
        String v = versionField != null ? versionField.getValue().trim() : "";
        if (v.isEmpty()) { dlStatus = t("easytier.core.enter_version"); buildCorePage(); return; }
        dlStatus = "Downloading " + v + "...";
        buildCorePage();
        NativeLoader.downloadUpdate(v, msg -> { dlStatus = msg; buildCorePage(); });
    }

    private void saveConfig() {
        if (hostnameField != null) config.hostname = hostnameField.getValue();
        if (networkNameField != null) config.networkName = networkNameField.getValue();
        if (networkSecretField != null) config.networkSecret = networkSecretField.getValue();
        config.save(FabricLoader.getInstance().getConfigDir());
    }

    private void saveAndClose() { saveConfig(); onClose(); }

    @Override
    public void onClose() {
        saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
    }
}
