package com.easytier.mcmod.screen;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.EasyTierModClient;
import com.easytier.mcmod.config.ModConfig;
import com.easytier.mcmod.easytier.NativeLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Configuration editing screen for EasyTier mod settings.
 * Accessible from ModMenu or the EasyTier management screen.
 */
public class EasyTierConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    private static final int LEFT_COL = 50;
    private static final int RIGHT_COL = 200;
    private static final int START_Y = 40;
    private static final int ROW_HEIGHT = 25;

    // Network settings
    private EditBox networkNameField;
    private EditBox networkSecretField;
    private EditBox listenUrlField;
    private EditBox rpcHostField;
    private EditBox rpcPortField;
    private CycleButton<Boolean> autoStartToggle;
    private CycleButton<Boolean> encryptionToggle;
    private CycleButton<Boolean> ipv6Toggle;
    private CycleButton<Boolean> latencyFirstToggle;
    private CycleButton<Boolean> kcpToggle;
    private CycleButton<Boolean> quicToggle;
    private CycleButton<Boolean> disableP2pToggle;
    private EditBox defaultProtocolField;

    // HUD settings
    private CycleButton<Boolean> hudEnabledToggle;
    private CycleButton<String> hudPositionCycle;
    private CycleButton<String> hudColorCycle;

    // Mirror settings
    private EditBox apiMirrorField;
    private EditBox downloadMirrorField;

    // Buttons
    private Button saveButton;
    private Button versionsButton;
    private EditBox downloadVersionField;
    private Button downloadButton;
    private Button startButton;
    private Button stopButton;

    public EasyTierConfigScreen(Screen parent) {
        super(Component.translatable("screen.easytier-mcmod.config"));
        this.parent = parent;
        this.config = EasyTierMod.getConfig();
    }

    @Override
    protected void init() {
        int y = START_Y;
        int width = this.width;

        // ---- Section: Network ----
        addRenderableOnly(new MultiLineTextWidget(
                LEFT_COL, y, Component.translatable("config.easytier-mcmod.section.network").withColor(0xFFAA00),
                this.font
        ));
        y += ROW_HEIGHT;

        // Network Name
        addLabel(LEFT_COL, y, "config.easytier-mcmod.network_name");
        networkNameField = addField(RIGHT_COL, y, 200, config.networkName);
        addRenderableWidget(networkNameField);
        y += ROW_HEIGHT;

        // Network Secret
        addLabel(LEFT_COL, y, "config.easytier-mcmod.network_secret");
        networkSecretField = addField(RIGHT_COL, y, 200, config.networkSecret);
        addRenderableWidget(networkSecretField);
        y += ROW_HEIGHT;

        // Listen URL
        addLabel(LEFT_COL, y, "config.easytier-mcmod.listen_url");
        listenUrlField = addField(RIGHT_COL, y, 300, config.listenUrl);
        addRenderableWidget(listenUrlField);
        y += ROW_HEIGHT;

        // RPC Host
        addLabel(LEFT_COL, y, "config.easytier-mcmod.rpc_host");
        rpcHostField = addField(RIGHT_COL, y, 150, config.rpcHost);
        addRenderableWidget(rpcHostField);
        y += ROW_HEIGHT;

        // RPC Port
        addLabel(LEFT_COL, y, "config.easytier-mcmod.rpc_port");
        rpcPortField = addField(RIGHT_COL, y, 80, String.valueOf(config.rpcPort));
        rpcPortField.setFilter(s -> s.matches("\\d*"));
        addRenderableWidget(rpcPortField);
        y += ROW_HEIGHT;

        // Default Protocol
        addLabel(LEFT_COL, y, "config.easytier-mcmod.default_protocol");
        defaultProtocolField = addField(RIGHT_COL, y, 80, config.defaultProtocol);
        addRenderableWidget(defaultProtocolField);
        y += ROW_HEIGHT;

        y += 5; // spacer

        // ---- Section: Flags ----
        addRenderableOnly(new MultiLineTextWidget(
                LEFT_COL, y, Component.translatable("config.easytier-mcmod.section.flags").withColor(0xFFAA00),
                this.font
        ));
        y += ROW_HEIGHT;

        // Toggles row 1
        autoStartToggle = addToggle(LEFT_COL, y, config.autoStart, "config.easytier-mcmod.auto_start");
        addRenderableWidget(autoStartToggle);
        encryptionToggle = addToggle(LEFT_COL + 160, y, config.enableEncryption, "config.easytier-mcmod.enable_encryption");
        addRenderableWidget(encryptionToggle);
        y += ROW_HEIGHT;

        // Toggles row 2
        ipv6Toggle = addToggle(LEFT_COL, y, config.enableIpv6, "config.easytier-mcmod.enable_ipv6");
        addRenderableWidget(ipv6Toggle);
        latencyFirstToggle = addToggle(LEFT_COL + 160, y, config.latencyFirst, "config.easytier-mcmod.latency_first");
        addRenderableWidget(latencyFirstToggle);
        y += ROW_HEIGHT;

        // Toggles row 3
        kcpToggle = addToggle(LEFT_COL, y, config.enableKcpProxy, "config.easytier-mcmod.enable_kcp");
        addRenderableWidget(kcpToggle);
        quicToggle = addToggle(LEFT_COL + 160, y, config.enableQuicProxy, "config.easytier-mcmod.enable_quic");
        addRenderableWidget(quicToggle);
        y += ROW_HEIGHT;

        // Toggles row 4
        disableP2pToggle = addToggle(LEFT_COL, y, config.disableP2p, "config.easytier-mcmod.disable_p2p");
        addRenderableWidget(disableP2pToggle);
        y += ROW_HEIGHT;

        y += 5; // spacer

        // ---- Section: HUD ----
        addRenderableOnly(new MultiLineTextWidget(
                LEFT_COL, y, Component.translatable("config.easytier-mcmod.section.hud").withColor(0xFFAA00),
                this.font
        ));
        y += ROW_HEIGHT;

        hudEnabledToggle = addToggle(LEFT_COL, y, config.hudEnabled, "config.easytier-mcmod.hud_enabled");
        addRenderableWidget(hudEnabledToggle);
        y += ROW_HEIGHT;

        addLabel(LEFT_COL, y, "config.easytier-mcmod.hud_position");
        hudPositionCycle = CycleButton.<String>builder(value -> {
            return switch (value) {
                case "top_left" -> Component.translatable("config.easytier-mcmod.hud_position.top_left");
                case "bottom_left" -> Component.translatable("config.easytier-mcmod.hud_position.bottom_left");
                case "bottom_right" -> Component.translatable("config.easytier-mcmod.hud_position.bottom_right");
                default -> Component.translatable("config.easytier-mcmod.hud_position.top_right");
            };
        }).withValues("top_left", "top_right", "bottom_left", "bottom_right")
                .withInitialValue(config.hudPosition)
                .create(RIGHT_COL, y, 150, 20,
                        Component.translatable("config.easytier-mcmod.hud_position"),
                        (btn, val) -> {});
        addRenderableWidget(hudPositionCycle);
        y += ROW_HEIGHT;

        addLabel(LEFT_COL, y, "config.easytier-mcmod.hud_color");
        hudColorCycle = CycleButton.<String>builder(value -> {
            return switch (value) {
                case "white" -> Component.translatable("config.easytier-mcmod.hud_color.white");
                case "yellow" -> Component.translatable("config.easytier-mcmod.hud_color.yellow");
                case "red" -> Component.translatable("config.easytier-mcmod.hud_color.red");
                case "cyan" -> Component.translatable("config.easytier-mcmod.hud_color.cyan");
                default -> Component.translatable("config.easytier-mcmod.hud_color.green");
            };
        }).withValues("green", "white", "yellow", "red", "cyan")
                .withInitialValue(colorToString(config.hudColor))
                .create(RIGHT_COL, y, 150, 20,
                        Component.translatable("config.easytier-mcmod.hud_color"),
                        (btn, val) -> {});
        addRenderableWidget(hudColorCycle);
        y += ROW_HEIGHT;

        y += 10;

        // ---- Section: Mirror ----
        addRenderableOnly(new MultiLineTextWidget(
                LEFT_COL, y, Component.translatable("config.easytier-mcmod.section.mirror").withColor(0xFFAA00),
                this.font
        ));
        y += ROW_HEIGHT;

        addLabel(LEFT_COL, y, "config.easytier-mcmod.api_mirror");
        apiMirrorField = addField(RIGHT_COL, y, 320, config.apiMirror);
        apiMirrorField.setHint(Component.literal("https://api.github.com (default)"));
        addRenderableWidget(apiMirrorField);
        y += ROW_HEIGHT;

        addLabel(LEFT_COL, y, "config.easytier-mcmod.download_mirror");
        downloadMirrorField = addField(RIGHT_COL, y, 320, config.downloadMirror);
        downloadMirrorField.setHint(Component.literal("GitHub Releases (default)"));
        addRenderableWidget(downloadMirrorField);
        y += ROW_HEIGHT + 5;

        // ---- Section: Download ----
        addRenderableOnly(new MultiLineTextWidget(
                LEFT_COL, y, Component.translatable("config.easytier-mcmod.section.download").withColor(0xFFAA00),
                this.font
        ));
        y += ROW_HEIGHT;

        // Version: [________] [Versions] [Download]
        versionsButton = Button.builder(
                Component.literal("Versions"),
                btn -> fetchVersions()
        ).bounds(LEFT_COL, y, 80, 20).build();
        addRenderableWidget(versionsButton);

        downloadVersionField = new EditBox(this.font, LEFT_COL + 85, y, 160, 20,
                Component.literal(""));
        downloadVersionField.setHint(Component.literal("e.g. v2.6.4"));
        addRenderableWidget(downloadVersionField);

        downloadButton = Button.builder(
                Component.literal("Download"),
                btn -> downloadVersion()
        ).bounds(LEFT_COL + 250, y, 80, 20).build();
        addRenderableWidget(downloadButton);
        y += ROW_HEIGHT;

        addRenderableOnly(new MultiLineTextWidget(LEFT_COL, y,
                Component.literal("§7Current: " + NativeLoader.getCurrentVersion()), this.font));
        y += ROW_HEIGHT + 5;

        // ---- Section: Process ----
        int btnWidth = 120;
        int btnSpacing = 5;

        startButton = Button.builder(
                Component.translatable("config.easytier-mcmod.start"),
                btn -> startEasyTier()
        ).bounds(LEFT_COL, y, btnWidth, 20).build();
        addRenderableWidget(startButton);

        stopButton = Button.builder(
                Component.translatable("config.easytier-mcmod.stop"),
                btn -> stopEasyTier()
        ).bounds(LEFT_COL + btnWidth + btnSpacing, y, btnWidth, 20).build();
        addRenderableWidget(stopButton);
        y += ROW_HEIGHT + 10;

        // Save button (bottom)
        saveButton = Button.builder(
                Component.translatable("config.easytier-mcmod.save"),
                btn -> saveConfig()
        ).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build();
        addRenderableWidget(saveButton);

        // Done button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> onClose()
        ).bounds(this.width / 2 - 100, this.height - 55, 200, 20).build());

        updateButtonStates();
    }

    private void addLabel(int x, int y, String translationKey) {
        addRenderableOnly(new MultiLineTextWidget(x, y,
                Component.translatable(translationKey), this.font));
    }

    private EditBox addField(int x, int y, int width, String initial) {
        EditBox field = new EditBox(this.font, x, y, width, 20,
                Component.empty());
        field.setValue(initial);
        field.setMaxLength(256);
        return field;
    }

    private CycleButton<Boolean> addToggle(int x, int y, boolean initial, String key) {
        return CycleButton.onOffBuilder(initial)
                .create(x, y, 150, 20, Component.translatable(key), (btn, val) -> {});
    }

    private void updateButtonStates() {
        var proc = EasyTierMod.getEasyTierProcess();
        boolean running = proc != null && proc.isRunning();
        if (startButton != null) startButton.active = !running;
        if (stopButton != null) stopButton.active = running;
    }

    private void saveConfig() {
        config.networkName = networkNameField.getValue();
        config.networkSecret = networkSecretField.getValue();
        config.listenUrl = listenUrlField.getValue();
        config.rpcHost = rpcHostField.getValue();
        try {
            config.rpcPort = Integer.parseInt(rpcPortField.getValue());
        } catch (NumberFormatException ignored) {}
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
        config.hudColor = stringToColor(hudColorCycle.getValue());

        config.save(FabricLoader.getInstance().getConfigDir());

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("message.easytier-mcmod.config_saved"), false);
        }
    }

    private void fetchVersions() {
        sendMessage("§6Fetching versions...");
        NativeLoader.fetchAvailableVersions().thenAccept(versions -> {
            if (versions.isEmpty()) {
                sendMessage("§cNo versions found. Check mirror settings.");
                return;
            }
            // Show top 5 in chat, fill version field with latest
            if (!versions.isEmpty() && downloadVersionField != null) {
                downloadVersionField.setValue(versions.get(0).tag);
            }
            StringBuilder sb = new StringBuilder("§6Versions: ");
            for (int i = 0; i < Math.min(5, versions.size()); i++) {
                if (i > 0) sb.append("§7, ");
                sb.append("§e").append(versions.get(i).tag);
            }
            sendMessage(sb.toString());
        }).exceptionally(e -> {
            sendMessage("§cFailed: " + e.getMessage());
            return null;
        });
    }

    private void downloadVersion() {
        String version = downloadVersionField.getValue();
        if (version.isEmpty()) {
            sendMessage("§cEnter a version tag (e.g. v2.6.4) or click 'Versions' to list");
            return;
        }
        sendMessage("§6Downloading " + version + "...");
        NativeLoader.downloadUpdate(version, msg -> {
            sendMessage("§e" + msg);
        }).thenAccept(success -> {
            if (success) {
                sendMessage("§aDownload complete! You can now start EasyTier.");
            } else {
                sendMessage("§cDownload failed. Check mirror or try another version.");
            }
        });
    }

    private void startEasyTier() {
        EasyTierMod.startEasyTier();
        updateButtonStates();
        sendMessage("§aStarting EasyTier...");
    }

    private void stopEasyTier() {
        EasyTierMod.stopEasyTier();
        updateButtonStates();
        sendMessage("§aStopping EasyTier...");
    }

    private void sendMessage(String msg) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(msg), false);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // Version info
        String version = "EasyTier: " + NativeLoader.getCurrentVersion();
        context.drawString(this.font, version, this.width - this.font.width(version) - 10,
                this.height - 12, 0x888888);
    }

    private static String colorToString(int color) {
        return switch (color) {
            case 0xFFFFFF -> "white";
            case 0xFFFF00 -> "yellow";
            case 0xFF0000 -> "red";
            case 0x00FFFF -> "cyan";
            default -> "green";
        };
    }

    private static int stringToColor(String str) {
        return switch (str) {
            case "white" -> 0xFFFFFF;
            case "yellow" -> 0xFFFF00;
            case "red" -> 0xFF0000;
            case "cyan" -> 0x00FFFF;
            default -> 0x00FF00;
        };
    }
}
