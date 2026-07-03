package com.easytier.mcmod;

import com.easytier.mcmod.command.EasyTierCommands;
import com.easytier.mcmod.config.ModConfig;
import com.easytier.mcmod.easytier.EasyTierProcess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyTierMod implements ModInitializer {
    public static final String MOD_ID = "easytier-mcmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static EasyTierProcess easyTierProcess;
    private static ModConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("[EasyTier] Initializing EasyTier Mod...");

        // Load or create default config
        config = ModConfig.load(FabricLoader.getInstance().getConfigDir());

        // Register commands (available on both client and server)
        EasyTierCommands.register();

        // Start EasyTier if enabled in config
        if (config.autoStart) {
            startEasyTier();
        }

        LOGGER.info("[EasyTier] Mod initialized successfully!");
    }

    public static EasyTierProcess getEasyTierProcess() {
        return easyTierProcess;
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static void startEasyTier() {
        if (easyTierProcess != null && easyTierProcess.isRunning()) {
            LOGGER.warn("[EasyTier] EasyTier is already running");
            return;
        }
        easyTierProcess = new EasyTierProcess(config);
        easyTierProcess.start();
    }

    public static void stopEasyTier() {
        if (easyTierProcess != null) {
            easyTierProcess.stop();
            easyTierProcess = null;
        }
    }
}
