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
 * EasyTier configuration screen — simple main page + advanced settings dialog.
 * Design follows EasytierGame: minimal first-level, advanced hidden behind button.
 */
public class EasyTierConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private EditBox hostnameField, networkNameField, networkSecretField, peerUrlField;
    private String connectorList = "";
    private String statusText = "";
    private Button startStopBtn;
    private boolean showingAdvanced;

    public EasyTierConfigScreen(Screen parent) {
        super(Component.literal("EasyTier"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override
    protected void init() {
        showingAdvanced = false;
        buildMain();
    }

    // ============== MAIN PAGE ==============
    private void buildMain() {
        clearWidgets();
        int cx = this.width / 2;
        int y = 30;

        // Title
        addRenderableWidget(new MultiLineTextWidget(cx - 80, y,
                Component.literal("EasyTier").withColor(0x00AAFF), this.font));
        y += 28;

        // ---- Version / Status ----
        String ver = NativeLoader.getCurrentVersion();
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        statusText = running ? "status_running" : ver.equals("not installed") ? "status_no_bin" : "status_stopped";
        addRenderableWidget(new MultiLineTextWidget(cx - 100, y,
                Component.literal(statusText), this.font));
        y += 18;

        // ---- Hostname ----
        addField(cx, y, 200, "hostname", "e.g. Player1", config.hostname,
                f -> hostnameField = f);
        y += 22;

        // ---- Network Name ----
        addField(cx, y, 200, "networkName", "Room Name", config.networkName,
                f -> networkNameField = f);
        y += 22;

        // ---- Network Secret ----
        addField(cx, y, 200, "networkSecret", "Room Password (optional)", config.networkSecret,
                f -> networkSecretField = f);
        y += 22;

        // ---- Peer URL ----
        addField(cx, y, 300, "peerUrl", "Server: tcp://host:port", "",
                f -> peerUrlField = f);
        y += 4;
        // Add / List connector buttons
        addButton(cx + 155, y, 30, "+", () -> addConnector());
        addButton(cx + 188, y, 40, "List", () -> refreshConnectors());
        y += 22;

        // Connector list text
        addRenderableWidget(new MultiLineTextWidget(cx - 150, y,
                Component.literal("§7" + connectorList), this.font));
        y += 16;

        y += 8;

        // ---- Start/Stop Button ----
        startStopBtn = Button.builder(
                Component.literal(running ? "§cStop" : "§aStart"),
                btn -> toggleProcess()
        ).bounds(cx - 60, y, 120, 24).build();
        addRenderableWidget(startStopBtn);
        y += 34;

        // ---- Bottom row buttons ----
        addRenderableWidget(Button.builder(
                Component.literal("Advanced"),
                btn -> buildAdvanced()
        ).bounds(cx - 100, this.height - 50, 70, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Core"),
                btn -> buildCorePage()
        ).bounds(cx - 25, this.height - 50, 50, 20).build());

        addButton(cx + 30, this.height - 50, 70, "Save", () -> saveAndClose());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> onClose()
        ).bounds(cx + 105, this.height - 50, 60, 20).build());
    }

    // ============== ADVANCED PAGE ==============
    private void buildAdvanced() {
        showingAdvanced = true;
        clearWidgets();
        int cx = this.width / 2;
        int y = 25;

        addTitle(cx, y, "Advanced Settings"); y += 24;

        // Flags
        addToggle(cx, y, "Encryption", config.enableEncryption, v -> config.enableEncryption = v); y += 20;
        addToggle(cx, y, "IPv6", config.enableIpv6, v -> config.enableIpv6 = v); y += 20;
        addToggle(cx, y, "Latency First", config.latencyFirst, v -> config.latencyFirst = v); y += 20;
        addToggle(cx, y, "KCP Proxy", config.enableKcpProxy, v -> config.enableKcpProxy = v); y += 20;
        addToggle(cx, y, "QUIC Proxy", config.enableQuicProxy, v -> config.enableQuicProxy = v); y += 20;
        addToggle(cx, y, "Disable P2P", config.disableP2p, v -> config.disableP2p = v); y += 22;

        // RPC
        addField(cx, y, 200, "rpcHost", "RPC Host", config.rpcHost, f -> {});
        y += 22;
        addField(cx, y, 60, "rpcPort", "RPC Port", String.valueOf(config.rpcPort), f -> {}); y += 22;

        // Listen URL
        addField(cx, y, 280, "listenUrl", "Listen URL", config.listenUrl, f -> {}); y += 22;

        // Default protocol
        addField(cx, y, 80, "defaultProto", "Protocol", config.defaultProtocol, f -> {}); y += 22;

        // Auto-start
        addToggle(cx, y, "Auto Start", config.autoStart, v -> config.autoStart = v); y += 20;

        // HUD
        addToggle(cx, y, "HUD", config.hudEnabled, v -> config.hudEnabled = v); y += 24;

        // Mirror
        addField(cx, y, 280, "apiMirror", "API Mirror", config.apiMirror, f -> config.apiMirror = f.getValue()); y += 22;
        addField(cx, y, 280, "dlMirror", "Download Mirror", config.downloadMirror, f -> config.downloadMirror = f.getValue()); y += 22;

        // Back
        addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> { saveConfig(); buildMain(); }
        ).bounds(cx - 80, this.height - 30, 70, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("Save"),
                btn -> { saveConfig(); buildMain(); }
        ).bounds(cx, this.height - 30, 70, 20).build());
    }

    // ============== CORE PAGE (Version/Download) ==============
    private EditBox versionField;
    private String dlStatus = "";

    private void buildCorePage() {
        showingAdvanced = true;
        clearWidgets();
        int cx = this.width / 2;
        int y = 25;

        addTitle(cx, y, "Core Management"); y += 24;
        addRenderableWidget(new MultiLineTextWidget(cx - 120, y,
                Component.literal("§eCurrent: " + NativeLoader.getCurrentVersion()), this.font));
        y += 18;

        // Version input
        versionField = new EditBox(this.font, cx - 80, y, 120, 20, Component.empty());
        versionField.setHint(Component.literal("v2.6.4"));
        addRenderableWidget(versionField);

        addButton(cx + 45, y, 55, "Versions", () -> fetchVersions());
        addButton(cx + 105, y, 60, "Download", () -> downloadVersion());
        y += 24;

        // Download status
        addRenderableWidget(new MultiLineTextWidget(cx - 130, y,
                Component.literal("§7" + dlStatus), this.font));
        y += 20;

        // Mirror field
        addField(cx, y, 260, "dlMirror2", "Download Mirror", config.downloadMirror,
                f -> config.downloadMirror = f.getValue());
        y += 24;

        addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> { saveConfig(); buildMain(); }
        ).bounds(cx - 40, this.height - 30, 80, 20).build());
    }

    // ============== Helpers ==============
    private void addTitle(int cx, int y, String text) {
        addRenderableWidget(new MultiLineTextWidget(cx - 60, y,
                Component.literal(text).withColor(0xFFAA00), this.font));
    }

    private void addField(int cx, int y, int w, String id, String hint, String value,
                          java.util.function.Consumer<EditBox> setter) {
        int x = cx - w / 2;
        EditBox field = new EditBox(this.font, x, y, w, 16, Component.empty());
        field.setValue(value != null ? value : "");
        field.setHint(Component.literal(hint));
        field.setMaxLength(256);
        addRenderableWidget(field);
        if (setter != null) setter.accept(field);
    }

    private void addToggle(int cx, int y, String label, boolean val, java.util.function.Consumer<Boolean> setter) {
        var btn = CycleButton.onOffBuilder(val).create(cx - 80, y, 160, 16,
                Component.literal(label), (b, v) -> setter.accept(v));
        addRenderableWidget(btn);
    }

    private void addButton(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), btn -> action.run())
                .bounds(x, y, w, 16).build());
    }

    // ============== Actions ==============
    private void toggleProcess() {
        var proc = EasyTierMod.getEasyTierProcess();
        if (proc != null && proc.isRunning()) {
            EasyTierMod.stopEasyTier();
        } else {
            EasyTierMod.startEasyTier();
        }
        buildMain();
    }

    private void addConnector() {
        String url = peerUrlField != null ? peerUrlField.getValue().trim() : "";
        if (url.isEmpty()) { statusText = "Enter a peer URL first"; buildMain(); return; }
        EasyTierCli.execute(config, "connector", "add", url)
                .thenAccept(r -> { connectorList = "Added: " + url; buildMain(); })
                .exceptionally(e -> { connectorList = "Error: " + e.getMessage(); buildMain(); return null; });
    }

    private void refreshConnectors() {
        connectorList = "Loading...";
        EasyTierCli.execute(config, "connector", "list").thenAccept(r -> {
            connectorList = r.trim().isEmpty() ? "(none)" : r.trim().lines().count() + " peers";
            buildMain();
        }).exceptionally(e -> { connectorList = "Error"; buildMain(); return null; });
    }

    private void fetchVersions() {
        dlStatus = "Fetching...";
        NativeLoader.fetchAvailableVersions().thenAccept(versions -> {
            if (!versions.isEmpty()) {
                versionField.setValue(versions.get(0).tag);
                var sb = new StringBuilder();
                for (int i = 0; i < Math.min(8, versions.size()); i++)
                    sb.append(versions.get(i).tag).append(" ");
                dlStatus = sb.toString();
            } else dlStatus = "No versions found";
            buildCorePage();
        }).exceptionally(e -> { dlStatus = "Error: " + e.getMessage(); buildCorePage(); return null; });
    }

    private void downloadVersion() {
        String v = versionField != null ? versionField.getValue().trim() : "";
        if (v.isEmpty()) { dlStatus = "Enter a version first"; buildCorePage(); return; }
        dlStatus = "Downloading " + v + "...";
        buildCorePage();
        NativeLoader.downloadUpdate(v, msg -> {
            dlStatus = msg;
            buildCorePage();
        });
    }

    private void saveConfig() {
        if (hostnameField != null) config.hostname = hostnameField.getValue();
        if (networkNameField != null) config.networkName = networkNameField.getValue();
        if (networkSecretField != null) config.networkSecret = networkSecretField.getValue();
        config.save(FabricLoader.getInstance().getConfigDir());
    }

    private void saveAndClose() {
        saveConfig();
        onClose();
    }

    @Override
    public void onClose() {
        saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }
}
