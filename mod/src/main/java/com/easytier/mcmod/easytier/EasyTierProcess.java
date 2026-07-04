package com.easytier.mcmod.easytier;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.config.ModConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages the easytier-core daemon process lifecycle.
 * Starts, monitors, and stops the bundled easytier-core binary.
 */
public class EasyTierProcess {
    private final ModConfig config;
    private Process process;
    private Thread outputReader;
    private volatile boolean running = false;
    private final List<String> recentLogs = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG_LINES = 500;

    public EasyTierProcess(ModConfig config) {
        this.config = config;
    }

    /**
     * Start easytier-core as a background process.
     */
    public void start() {
        if (running) {
            EasyTierMod.LOGGER.warn("[EasyTier] Process already running");
            return;
        }

        try {
            Path binDir = NativeLoader.getBinDir();
            String binaryName = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "easytier-core.exe" : "easytier-core";
            Path binaryPath = binDir.resolve(binaryName);

            if (!java.nio.file.Files.exists(binaryPath)) {
                EasyTierMod.LOGGER.error("[EasyTier] Binary not found at {}. Run /easytier install first.", binaryPath);
                return;
            }

            List<String> command = new ArrayList<>();
            command.add(binaryPath.toAbsolutePath().toString());
            for (String arg : config.buildCoreArgs()) {
                command.add(arg);
            }

            String fullCmd = String.join(" ", command);
            EasyTierMod.LOGGER.info("[EasyTier] Starting: {}", fullCmd);
            recentLogs.add("> " + fullCmd);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(binDir.getParent().toFile());
            pb.redirectErrorStream(true);

            process = pb.start();
            running = true;
            recentLogs.add("Process started (PID: " + process.pid() + ")");

            // Read stdout in background
            outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        recentLogs.add(line);
                        if (recentLogs.size() > MAX_LOG_LINES) {
                            recentLogs.removeFirst();
                        }
                        // Notify listeners
                        for (Consumer<String> listener : logListeners) {
                            try { listener.accept(line); } catch (Exception ignored) {}
                        }
                        EasyTierMod.LOGGER.debug("[easytier-core] {}", line);
                    }
                } catch (IOException e) {
                    if (running) {
                        EasyTierMod.LOGGER.error("[EasyTier] Output reader error: {}", e.getMessage());
                    }
                }
            }, "EasyTier-stdout");
            outputReader.setDaemon(true);
            outputReader.start();

            // Monitor process exit
            Thread monitor = new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    running = false;
                    recentLogs.add("Process exited with code " + exitCode);
                    EasyTierMod.LOGGER.info("[EasyTier] Process exited with code {}", exitCode);
                } catch (InterruptedException ignored) {}
            }, "EasyTier-monitor");
            monitor.setDaemon(true);
            monitor.start();

            EasyTierMod.LOGGER.info("[EasyTier] Process started successfully");
        } catch (IOException e) {
            running = false;
            EasyTierMod.LOGGER.error("[EasyTier] Failed to start process: {}", e.getMessage());
        }
    }

    /**
     * Stop the easytier-core process gracefully.
     */
    public void stop() {
        if (!running || process == null) {
            return;
        }

        EasyTierMod.LOGGER.info("[EasyTier] Stopping process...");
        running = false;

        // Try graceful shutdown first (SIGTERM / Ctrl-C equivalent)
        process.destroy();

        // Wait a bit, then force kill if still alive
        try {
            if (process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                EasyTierMod.LOGGER.info("[EasyTier] Process stopped gracefully");
            } else {
                EasyTierMod.LOGGER.warn("[EasyTier] Process did not stop, force killing...");
                process.destroyForcibly();
                process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }

        if (outputReader != null) {
            outputReader.interrupt();
            outputReader = null;
        }

        process = null;
        EasyTierMod.LOGGER.info("[EasyTier] Process stopped");
    }

    /**
     * Restart the EasyTier process (stop then start).
     */
    public void restart() {
        EasyTierMod.LOGGER.info("[EasyTier] Restarting process...");
        stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
        start();
    }

    public boolean isRunning() {
        return running && process != null && process.isAlive();
    }

    /**
     * Get recent log output from the process.
     */
    public List<String> getRecentLogs(int maxLines) {
        int size = recentLogs.size();
        if (size <= maxLines) {
            return new ArrayList<>(recentLogs);
        }
        return new ArrayList<>(recentLogs.subList(size - maxLines, size));
    }

    /**
     * Add a listener for real-time log output.
     */
    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    /**
     * Remove a log listener.
     */
    public void removeLogListener(Consumer<String> listener) {
        logListeners.remove(listener);
    }
}
