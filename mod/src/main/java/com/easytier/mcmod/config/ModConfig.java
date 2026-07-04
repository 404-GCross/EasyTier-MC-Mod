package com.easytier.mcmod.config;

import com.easytier.mcmod.EasyTierMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "easytier-mcmod.json";

    // ---- EasyTier connection settings ----
    public String rpcHost = "127.0.0.1";
    public int rpcPort = 15888;

    // ---- EasyTier process settings ----
    public boolean autoStart = true;
    public String hostname = "";
    public String networkName = "easytier-mc";
    public String networkSecret = "";
    public String listenUrl = "tcp://0.0.0.0:11010";
    public String peers = "";
    public String ipv4 = "";
    public boolean dhcp = true;
    public boolean enableEncryption = true;
    public boolean enableIpv6 = true;

    // ---- EasyTier advanced flags ----
    public boolean latencyFirst = false;
    public String defaultProtocol = "tcp";
    public boolean enableKcpProxy = false;
    public boolean enableQuicProxy = false;
    public boolean disableP2p = false;
    public boolean noTun = true;    // Use smoltcp instead of TUN device (no admin needed)
    public boolean useSmoltcp = true;

    // ---- HUD settings ----
    public boolean hudEnabled = true;
    public String hudPosition = "top_right"; // top_left, top_right, bottom_left, bottom_right
    public int hudColor = 0x00FF00; // green

    // ---- Internal paths ----
    public transient Path easyTierDir; // resolved at runtime

    public static ModConfig load(Path configDir) {
        Path configFile = configDir.resolve(FILE_NAME);
        ModConfig config;

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                config = GSON.fromJson(json, ModConfig.class);
                EasyTierMod.LOGGER.info("[EasyTier] Config loaded from {}", configFile);
            } catch (IOException e) {
                EasyTierMod.LOGGER.error("[EasyTier] Failed to load config, using defaults", e);
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
            config.save(configDir);
        }

        // Resolve internal paths
        config.easyTierDir = FabricLoader.getInstance().getGameDir().resolve("easytier");
        return config;
    }

    public void save(Path configDir) {
        Path configFile = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            String json = GSON.toJson(this);
            Files.writeString(configFile, json);
            EasyTierMod.LOGGER.info("[EasyTier] Config saved to {}", configFile);
        } catch (IOException e) {
            EasyTierMod.LOGGER.error("[EasyTier] Failed to save config", e);
        }
    }

    /**
     * Build EasyTier command-line arguments from config.
     */
    public String[] buildCoreArgs() {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("--network-name");
        args.add(networkName);
        if (!networkSecret.isEmpty()) {
            args.add("--network-secret");
            args.add(networkSecret);
        }
        if (!hostname.isEmpty()) {
            args.add("--hostname");
            args.add(hostname);
        }
        if (dhcp) {
            args.add("-d");
        } else if (!ipv4.isEmpty()) {
            args.add("--ipv4");
            args.add(ipv4.trim());
        }
        args.add("--default-protocol");
        args.add(defaultProtocol);
        args.add("--dev-name");
        args.add("easytier0");
        args.add("--rpc-portal");
        args.add(rpcHost + ":" + rpcPort);
        if (enableEncryption) {
            args.add("--enable-encryption");
        }
        if (enableIpv6) {
            args.add("--enable-ipv6");
        }
        if (latencyFirst) {
            args.add("--latency-first");
        }
        if (enableKcpProxy) {
            args.add("--enable-kcp-proxy");
        }
        if (enableQuicProxy) {
            args.add("--enable-quic-proxy");
        }
        if (disableP2p) {
            args.add("--disable-p2p");
        }
        if (noTun) {
            args.add("--no-tun");
        }
        if (useSmoltcp) {
            args.add("--use-smoltcp");
        }
        if (!listenUrl.isEmpty()) {
            for (String url : listenUrl.split(",")) {
                args.add("-l");
                args.add(url.trim());
            }
        }
        if (!peers.isEmpty()) {
            for (String p : peers.split(",")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) { args.add("--peers"); args.add(trimmed); }
            }
        }
        return args.toArray(new String[0]);
    }
}
