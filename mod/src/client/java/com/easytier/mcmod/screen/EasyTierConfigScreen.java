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

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable configuration screen for EasyTier mod settings.
 */
public class EasyTierConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    private static final int LEFT_COL = 15;
    private static final int COL2 = 160;
    private static final int ROW_HEIGHT = 24;
    private int contentHeight = 0;
    private int scrollY = 0;
    private int maxScroll = 0;

    // Network
    private EditBox networkNameField, networkSecretField, hostnameField, listenUrlField, rpcHostField, rpcPortField, defaultProtocolField;
    private CycleButton<Boolean> autoStartToggle, encryptionToggle, ipv6Toggle, latencyFirstToggle, kcpToggle, quicToggle, disableP2pToggle;
    // HUD
    private CycleButton<Boolean> hudEnabledToggle;
    private CycleButton<String> hudPositionCycle, hudColorCycle;
    // Mirror
    private EditBox apiMirrorField, downloadMirrorField;
    // Connector
    private String connectorListText = "(click List to refresh)";
    private EditBox connectorUrlField, removeUrlField;
    // Download
    private EditBox downloadVersionField;

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("screen.easytier-mcmod.config"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override
    protected void init() {
        scrollY = 0;
        buildWidgets();
        addFixedButtons();
    }

    private void buildWidgets() {
        int y = 15;

        // ==== Network Settings ====
        y = section(y, "config.easytier-mcmod.section.network");
        networkNameField = addField(y, "config.easytier-mcmod.network_name", config.networkName, 180); y += ROW_HEIGHT;
        networkSecretField = addField(y, "config.easytier-mcmod.network_secret", config.networkSecret, 180); y += ROW_HEIGHT;
        hostnameField = addField(y, "config.easytier-mcmod.hostname", config.hostname, 150); y += ROW_HEIGHT;
        listenUrlField = addField(y, "config.easytier-mcmod.listen_url", config.listenUrl, 280); y += ROW_HEIGHT;
        rpcHostField = addField(y, "config.easytier-mcmod.rpc_host", config.rpcHost, 120); y += ROW_HEIGHT;
        rpcPortField = addField(y, "config.easytier-mcmod.rpc_port", String.valueOf(config.rpcPort), 60);
        rpcPortField.setFilter(s -> s.matches("\\d*")); y += ROW_HEIGHT;
        defaultProtocolField = addField(y, "config.easytier-mcmod.default_protocol", config.defaultProtocol, 60); y += ROW_HEIGHT;

        // ==== Flags ====
        y = section(y, "config.easytier-mcmod.section.flags");
        autoStartToggle = toggle(COL2, y, config.autoStart, "Auto Start"); y += ROW_HEIGHT;
        encryptionToggle = toggle(COL2, y, config.enableEncryption, "Encryption"); y += ROW_HEIGHT;
        ipv6Toggle = toggle(COL2, y, config.enableIpv6, "IPv6"); y += ROW_HEIGHT;
        latencyFirstToggle = toggle(COL2, y, config.latencyFirst, "Latency First"); y += ROW_HEIGHT;
        kcpToggle = toggle(COL2, y, config.enableKcpProxy, "KCP Proxy"); y += ROW_HEIGHT;
        quicToggle = toggle(COL2, y, config.enableQuicProxy, "QUIC Proxy"); y += ROW_HEIGHT;
        disableP2pToggle = toggle(COL2, y, config.disableP2p, "Disable P2P"); y += ROW_HEIGHT;

        // ==== HUD ====
        y = section(y, "config.easytier-mcmod.section.hud");
        hudEnabledToggle = toggle(COL2, y, config.hudEnabled, "HUD Enabled"); y += ROW_HEIGHT;
        hudPositionCycle = cycle(y, "HUD Pos", List.of("top_left","top_right","bottom_left","bottom_right"), config.hudPosition); y += ROW_HEIGHT;
        hudColorCycle = cycle(y, "HUD Color", List.of("green","white","yellow","red","cyan"), colorToString(config.hudColor)); y += ROW_HEIGHT;

        // ==== Mirror ====
        y = section(y, "config.easytier-mcmod.section.mirror");
        apiMirrorField = addField(y, "config.easytier-mcmod.api_mirror", config.apiMirror, 300);
        apiMirrorField.setHint(Component.literal("api.github.com (default)")); y += ROW_HEIGHT;
        downloadMirrorField = addField(y, "config.easytier-mcmod.download_mirror", config.downloadMirror, 300);
        downloadMirrorField.setHint(Component.literal("GitHub Releases (default)")); y += ROW_HEIGHT;

        // ==== Peer Connectors ====
        y = section(y, "config.easytier-mcmod.section.peers");
        connectorUrlField = new EditBox(this.font, COL2, y, 200, 20, Component.empty());
        connectorUrlField.setHint(Component.literal("tcp://peer-ip:11010"));
        addRenderableWidget(connectorUrlField);
        addButton(COL2 + 205, y, 35, "Add", () -> addConnector(connectorUrlField.getValue()));
        addButton(COL2 + 243, y, 35, "List", () -> refreshConnectors());
        y += ROW_HEIGHT;
        removeUrlField = new EditBox(this.font, COL2, y, 200, 20, Component.empty());
        removeUrlField.setHint(Component.literal("URL to remove..."));
        addRenderableWidget(removeUrlField);
        addButton(COL2 + 205, y, 35, "Del", () -> removeConnector(removeUrlField.getValue()));
        y += ROW_HEIGHT;
        addRenderableOnly(new MultiLineTextWidget(COL2, y, Component.literal("§7" + connectorListText), this.font));
        y += 18;

        // ==== Download ====
        y = section(y, "config.easytier-mcmod.section.download");
        downloadVersionField = new EditBox(this.font, COL2, y, 140, 20, Component.empty());
        downloadVersionField.setHint(Component.literal("e.g. v2.6.4"));
        addRenderableWidget(downloadVersionField);
        addButton(COL2 + 145, y, 55, "Versions", () -> fetchVersions());
        addButton(COL2 + 205, y, 60, "Download", () -> downloadVersion());
        y += ROW_HEIGHT;
        addRenderableOnly(new MultiLineTextWidget(COL2, y,
                Component.literal("§7Current: " + NativeLoader.getCurrentVersion()), this.font));
        y += ROW_HEIGHT;

        // ==== Process ====
        y = section(y, null);
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        addButton(COL2, y, 80, running ? "§aRunning" : "Start", () -> {
            if (running) return;
            EasyTierMod.startEasyTier();
            this.minecraft.setScreen(new EasyTierConfigScreen(parent));
        });
        addButton(COL2 + 85, y, 60, "Stop", () -> { EasyTierMod.stopEasyTier();
            this.minecraft.setScreen(new EasyTierConfigScreen(parent)); });
        addButton(COL2 + 150, y, 80, "Restart", () -> {
            var p = EasyTierMod.getEasyTierProcess();
            if (p != null) p.restart();
            else EasyTierMod.startEasyTier();
            this.minecraft.setScreen(new EasyTierConfigScreen(parent));
        });
        y += ROW_HEIGHT + 5;

        contentHeight = y + 40;
        maxScroll = Math.max(0, contentHeight - this.height + 60);
    }

    // ==== Scrolling ====
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollY = (int) Math.clamp(scrollY - verticalAmount * 20, 0, maxScroll);
        rebuildWithScroll();
        return true;
    }

    private void rebuildWithScroll() {
        var saved = saveFieldValues();
        clearWidgets();
        buildWidgets();
        restoreFieldValues(saved);
        // Offset ALL renderable/includeable widgets by scroll amount
        var all = new ArrayList<net.minecraft.client.gui.components.Renderable>();
        all.addAll(renderables);
        for (var w : all) {
            if (w instanceof AbstractWidget aw) {
                aw.setY(aw.getY() - scrollY);
            }
        }
        addFixedButtons();
    }

    private java.util.Map<String, String> saveFieldValues() {
        var m = new java.util.HashMap<String, String>();
        if (networkNameField != null) m.put("nn", networkNameField.getValue());
        if (networkSecretField != null) m.put("ns", networkSecretField.getValue());
        if (hostnameField != null) m.put("hn", hostnameField.getValue());
        if (listenUrlField != null) m.put("lu", listenUrlField.getValue());
        if (rpcHostField != null) m.put("rh", rpcHostField.getValue());
        if (rpcPortField != null) m.put("rp", rpcPortField.getValue());
        if (defaultProtocolField != null) m.put("dp", defaultProtocolField.getValue());
        if (apiMirrorField != null) m.put("am", apiMirrorField.getValue());
        if (downloadMirrorField != null) m.put("dm", downloadMirrorField.getValue());
        if (downloadVersionField != null) m.put("dv", downloadVersionField.getValue());
        if (connectorUrlField != null) m.put("cu", connectorUrlField.getValue());
        if (removeUrlField != null) m.put("ru", removeUrlField.getValue());
        return m;
    }

    private void restoreFieldValues(java.util.Map<String, String> m) {
        if (networkNameField != null && m.containsKey("nn")) networkNameField.setValue(m.get("nn"));
        if (networkSecretField != null && m.containsKey("ns")) networkSecretField.setValue(m.get("ns"));
        if (hostnameField != null && m.containsKey("hn")) hostnameField.setValue(m.get("hn"));
        if (listenUrlField != null && m.containsKey("lu")) listenUrlField.setValue(m.get("lu"));
        if (rpcHostField != null && m.containsKey("rh")) rpcHostField.setValue(m.get("rh"));
        if (rpcPortField != null && m.containsKey("rp")) rpcPortField.setValue(m.get("rp"));
        if (defaultProtocolField != null && m.containsKey("dp")) defaultProtocolField.setValue(m.get("dp"));
        if (apiMirrorField != null && m.containsKey("am")) apiMirrorField.setValue(m.get("am"));
        if (downloadMirrorField != null && m.containsKey("dm")) downloadMirrorField.setValue(m.get("dm"));
        if (downloadVersionField != null && m.containsKey("dv")) downloadVersionField.setValue(m.get("dv"));
        if (connectorUrlField != null && m.containsKey("cu")) connectorUrlField.setValue(m.get("cu"));
        if (removeUrlField != null && m.containsKey("ru")) removeUrlField.setValue(m.get("ru"));
    }

    private void addFixedButtons() {
        addRenderableWidget(Button.builder(Component.translatable("config.easytier-mcmod.save"),
                btn -> saveConfig()).bounds(this.width / 2 - 105, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                btn -> onClose()).bounds(this.width / 2 + 5, this.height - 28, 100, 20).build());
    }

    // ==== Widget helpers ====
    private int section(int y, String key) {
        if (key != null) {
            addRenderableOnly(new MultiLineTextWidget(LEFT_COL, y,
                    Component.translatable(key).withColor(0xFFAA00), this.font));
        }
        return y + ROW_HEIGHT;
    }

    private EditBox addField(int y, String key, String value, int w) {
        addRenderableOnly(new MultiLineTextWidget(LEFT_COL, y, Component.translatable(key), this.font));
        EditBox f = new EditBox(this.font, COL2, y, w, 20, Component.empty());
        f.setValue(value != null ? value : ""); f.setMaxLength(256);
        addRenderableWidget(f);
        return f;
    }

    private CycleButton<Boolean> toggle(int x, int y, boolean init, String label) {
        var btn = CycleButton.onOffBuilder(init).create(x, y, 130, 20, Component.literal(label), (b, v) -> {});
        addRenderableWidget(btn);
        return btn;
    }

    private CycleButton<String> cycle(int y, String label, List<String> values, String init) {
        var btn = CycleButton.<String>builder(v -> Component.literal(v))
                .withValues(values).withInitialValue(init)
                .create(COL2, y, 140, 20, Component.literal(label), (b, v) -> {});
        addRenderableWidget(btn);
        return btn;
    }

    private void addButton(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), btn -> action.run())
                .bounds(x, y, w, 20).build());
    }

    // ==== Connector management ====
    private void refreshConnectors() {
        connectorListText = "Fetching...";
        EasyTierCli.execute(EasyTierMod.getConfig(), "connector", "list").thenAccept(result -> {
            if (result.contains("connector")) {
                connectorListText = result.trim().lines().count() + " connectors. Use List to refresh.";
            } else {
                connectorListText = result.trim();
            }
        }).exceptionally(e -> { connectorListText = "Error: " + e.getMessage(); return null; });
    }

    private void addConnector(String url) {
        if (url.isBlank()) { sendMsg("§cEnter a URL like tcp://1.2.3.4:11010"); return; }
        sendMsg("§6Adding: " + url);
        EasyTierCli.execute(EasyTierMod.getConfig(), "connector", "add", url)
                .thenAccept(r -> { sendMsg("§aAdded!"); refreshConnectors(); })
                .exceptionally(e -> { sendMsg("§cFailed: " + e.getMessage()); return null; });
    }

    private void removeConnector(String url) {
        if (url.isBlank()) { sendMsg("§cEnter the URL to remove"); return; }
        EasyTierCli.execute(EasyTierMod.getConfig(), "connector", "remove", url)
                .thenAccept(r -> { sendMsg("§aRemoved!"); refreshConnectors(); })
                .exceptionally(e -> { sendMsg("§cFailed: " + e.getMessage()); return null; });
    }

    // ==== Download ====
    private void fetchVersions() {
        sendMsg("§6Fetching versions...");
        NativeLoader.fetchAvailableVersions().thenAccept(versions -> {
            if (versions.isEmpty()) { sendMsg("§cNo versions found."); return; }
            downloadVersionField.setValue(versions.get(0).tag);
            var sb = new StringBuilder("§6Versions: ");
            for (int i = 0; i < Math.min(5, versions.size()); i++) {
                if (i > 0) sb.append("§7, ");
                sb.append("§e").append(versions.get(i).tag);
            }
            sendMsg(sb.toString());
        }).exceptionally(e -> { sendMsg("§c" + e.getMessage()); return null; });
    }

    private void downloadVersion() {
        String v = downloadVersionField.getValue();
        if (v.isEmpty()) { sendMsg("§cEnter a version first"); return; }
        sendMsg("§6Downloading " + v + "...");
        NativeLoader.downloadUpdate(v, this::sendMsg).thenAccept(ok -> {
            if (ok) sendMsg("§aDone! Start EasyTier to use.");
        });
    }

    private void sendMsg(String msg) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(msg), false);
        }
    }

    // ==== Save ====
    private void saveConfig() {
        config.networkName = networkNameField.getValue();
        config.networkSecret = networkSecretField.getValue();
        config.hostname = hostnameField.getValue();
        config.listenUrl = listenUrlField.getValue();
        config.rpcHost = rpcHostField.getValue();
        try { config.rpcPort = Integer.parseInt(rpcPortField.getValue()); } catch (NumberFormatException ignored) {}
        config.defaultProtocol = defaultProtocolField.getValue();
        config.autoStart = autoStartToggle.getValue();
        config.enableEncryption = encryptionToggle.getValue();
        config.enableIpv6 = ipv6Toggle.getValue();
        config.latencyFirst = latencyFirstToggle.getValue();
        config.enableKcpProxy = kcpToggle.getValue();
        config.enableQuicProxy = quicToggle.getValue();
        config.disableP2p = disableP2pToggle.getValue();
        config.apiMirror = apiMirrorField.getValue();
        config.downloadMirror = downloadMirrorField.getValue();
        config.hudEnabled = hudEnabledToggle.getValue();
        config.hudPosition = hudPositionCycle.getValue();
        config.hudColor = stringToInt(hudColorCycle.getValue());
        config.save(FabricLoader.getInstance().getConfigDir());
        sendMsg("§aConfig saved!");
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFF);

        // Scroll indicator (in the visible area above buttons)
        if (maxScroll > 0) {
            int visH = this.height - 65;
            int barH = Math.max(15, (int)((float)visH / (contentHeight) * visH));
            int barY = (int)((float)scrollY / maxScroll * (visH - barH));
            ctx.fill(this.width - 4, barY, this.width - 1, barY + barH, 0x66FFFFFF);
        }
    }

    private static String colorToString(int c) {
        return switch (c) { case 0xFFFFFF -> "white"; case 0xFFFF00 -> "yellow"; case 0xFF0000 -> "red"; case 0x00FFFF -> "cyan"; default -> "green"; };
    }
    private static int stringToInt(String s) {
        return switch (s) { case "white" -> 0xFFFFFF; case "yellow" -> 0xFFFF00; case "red" -> 0xFF0000; case "cyan" -> 0x00FFFF; default -> 0x00FF00; };
    }
}
