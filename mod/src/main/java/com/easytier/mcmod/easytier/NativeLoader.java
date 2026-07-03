package com.easytier.mcmod.easytier;

import com.easytier.mcmod.EasyTierMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages EasyTier native binary location and version detection.
 * User must manually place easytier-core + easytier-cli in .minecraft/easytier/bin/
 */
public class NativeLoader {
    private static final String BINARY_NAME = isWindows() ? "easytier-core.exe" : "easytier-core";
    private static final String CLI_NAME = isWindows() ? "easytier-cli.exe" : "easytier-cli";
    private static final String VERSION_FILE = "version.txt";

    private static Path binDir = null;
    private static String currentVersion = null;

    public static Path getBinDir() throws IOException {
        if (binDir != null) return binDir;
        Path gameDir = FabricLoader.getInstance().getGameDir();
        binDir = gameDir.resolve("easytier").resolve("bin");
        Files.createDirectories(binDir);
        // Check if we can detect version from the binary
        if (Files.exists(binDir.resolve(BINARY_NAME))) {
            makeExecutable(binDir.resolve(BINARY_NAME));
            makeExecutable(binDir.resolve(CLI_NAME));
            currentVersion = readVersionFromBinary();
        }
        return binDir;
    }

    public static boolean isInstalled() {
        try {
            return Files.exists(getBinDir().resolve(BINARY_NAME));
        } catch (IOException e) {
            return false;
        }
    }

    /** Get version by running easytier-core --version */
    public static String getCurrentVersion() {
        if (currentVersion != null && !currentVersion.equals("none")) return currentVersion;
        currentVersion = readVersionFromBinary();
        return currentVersion != null ? currentVersion : "not installed";
    }

    private static String readVersionFromBinary() {
        try {
            if (!isInstalled()) return null;
            Path corePath = getBinDir().resolve(BINARY_NAME);
            ProcessBuilder pb = new ProcessBuilder(corePath.toAbsolutePath().toString(), "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (line != null) {
                    // "easytier-core 2.6.4" -> "2.6.4"
                    return line.replace("easytier-core ", "").trim();
                }
            }
        } catch (Exception e) {
            EasyTierMod.LOGGER.debug("[EasyTier] Could not read version from binary: {}", e.getMessage());
        }
        return null;
    }

    public static String getPlatformId() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String osPart = os.contains("win") ? "windows" : os.contains("mac") || os.contains("darwin") ? "macos"
                : os.contains("linux") ? "linux" : os.contains("freebsd") ? "freebsd" : "linux";
        String archPart = arch.contains("amd64") || arch.contains("x86_64") ? "x86_64"
                : arch.contains("aarch64") || arch.contains("arm64") ? "aarch64"
                : arch.contains("arm") ? "arm" : arch.contains("mips") ? "mips" : "x86_64";
        return osPart + "-" + archPart;
    }

    private static void makeExecutable(Path path) {
        if (!isWindows() && Files.exists(path)) {
            try {
                Files.setPosixFilePermissions(path, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
            } catch (IOException ignored) {}
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static void uninstall() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path easyTierDir = gameDir.resolve("easytier");
        if (Files.exists(easyTierDir)) {
            try (Stream<Path> walk = Files.walk(easyTierDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
        binDir = null;
        currentVersion = null;
    }
}
