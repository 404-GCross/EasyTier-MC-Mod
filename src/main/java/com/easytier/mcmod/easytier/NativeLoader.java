package com.easytier.mcmod.easytier;

import com.easytier.mcmod.EasyTierMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Handles native EasyTier binary installation and updates.
 * On first run, extracts bundled binaries. Supports downloading updates from GitHub releases.
 */
public class NativeLoader {
    private static final String BINARY_NAME = isWindows() ? "easytier-core.exe" : "easytier-core";
    private static final String CLI_NAME = isWindows() ? "easytier-cli.exe" : "easytier-cli";
    private static final String VERSION_FILE = "version.txt";

    private static final String GITHUB_API = "https://api.github.com/repos/EasyTier/EasyTier/releases/latest";
    private static final String DOWNLOAD_BASE = "https://github.com/EasyTier/EasyTier/releases/latest/download";

    private static Path binDir = null;
    private static String currentVersion = null;

    /**
     * Get or initialize the binary directory. Extracts bundled binaries on first access.
     */
    public static Path getBinDir() throws IOException {
        if (binDir != null) return binDir;

        Path gameDir = FabricLoader.getInstance().getGameDir();
        binDir = gameDir.resolve("easytier").resolve("bin");
        Files.createDirectories(binDir);

        // Check if binaries need extraction
        Path corePath = binDir.resolve(BINARY_NAME);
        Path versionPath = binDir.resolve(VERSION_FILE);

        if (!Files.exists(corePath)) {
            EasyTierMod.LOGGER.info("[EasyTier] First run — extracting bundled native binaries...");
            extractBundledBinary("easytier-core", BINARY_NAME, corePath);
            extractBundledBinary("easytier-cli", CLI_NAME, binDir.resolve(CLI_NAME));
            // Write initial version
            Files.writeString(versionPath, "bundled");
            currentVersion = "bundled";
        } else {
            makeExecutable(corePath);
            makeExecutable(binDir.resolve(CLI_NAME));
            if (Files.exists(versionPath)) {
                currentVersion = Files.readString(versionPath).trim();
            }
        }

        return binDir;
    }

    /**
     * Get current EasyTier version string.
     */
    public static String getCurrentVersion() {
        if (currentVersion != null) return currentVersion;
        try {
            Path versionPath = getBinDir().resolve(VERSION_FILE);
            if (Files.exists(versionPath)) {
                currentVersion = Files.readString(versionPath).trim();
                return currentVersion;
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    /**
     * Extract a bundled native binary from the JAR resources.
     */
    private static void extractBundledBinary(String binaryName, String targetName, Path targetPath) throws IOException {
        String resourcePath = "/assets/easytier-mcmod/natives/" + getPlatformId() + "/" + binaryName;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                EasyTierMod.LOGGER.warn("[EasyTier] Bundled binary '{}' not found for platform '{}' — will need download",
                        binaryName, getPlatformId());
                return;
            }
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            makeExecutable(targetPath);
            EasyTierMod.LOGGER.info("[EasyTier] Extracted: {}", targetPath);
        }
    }

    /**
     * Get the platform identifier for native binary selection.
     */
    public static String getPlatformId() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String osPart;
        if (os.contains("win")) {
            osPart = "windows";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osPart = "macos";
        } else if (os.contains("linux")) {
            osPart = "linux";
        } else if (os.contains("freebsd")) {
            osPart = "freebsd";
        } else {
            osPart = "linux"; // fallback
        }

        String archPart;
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            archPart = "x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            archPart = "aarch64";
        } else if (arch.contains("arm")) {
            archPart = "arm";
        } else if (arch.contains("mips")) {
            archPart = "mips";
        } else {
            archPart = "x86_64"; // fallback
        }

        return osPart + "-" + archPart;
    }

    /**
     * Make a file executable (Unix/macOS only).
     */
    private static void makeExecutable(Path path) {
        if (!isWindows() && Files.exists(path)) {
            try {
                Files.setPosixFilePermissions(path, Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE
                ));
            } catch (IOException e) {
                EasyTierMod.LOGGER.warn("[EasyTier] Failed to set executable permission: {}", e.getMessage());
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Check if an update is available on GitHub.
     * @return a CompletableFuture with the latest version tag (e.g. "v2.6.4") or null if up-to-date
     */
    public static CompletableFuture<String> checkForUpdate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GITHUB_API))
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .timeout(java.time.Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return null;

                // Parse JSON manually to avoid dependencies
                String body = response.body();
                String tagName = extractJsonString(body, "tag_name");
                if (tagName == null || tagName.isEmpty()) return null;

                // Compare versions
                String current = getCurrentVersion();
                if (current.equals(tagName) || current.equals("bundled") && tagName != null) {
                    // For "bundled", check if the bundled version is older than latest
                    // Simple heuristic: return the latest version for user to decide
                    return tagName.equals(current) ? null : tagName;
                }
                return tagName.equals(current) ? null : tagName;
            } catch (Exception e) {
                EasyTierMod.LOGGER.warn("[EasyTier] Failed to check for updates: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * Download and install the latest EasyTier binaries from GitHub.
     * @param versionTag e.g. "v2.6.4"
     * @param progressCallback receives progress messages
     * @return CompletableFuture that completes when installation is done
     */
    public static CompletableFuture<Boolean> downloadUpdate(String versionTag, java.util.function.Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path binDir = getBinDir();
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(java.time.Duration.ofSeconds(15))
                        .build();

                String platformId = getPlatformId();
                // EasyTier release asset naming pattern
                String coreAsset = "easytier-core-" + platformId + (isWindows() ? ".exe" : "");
                String cliAsset = "easytier-cli-" + platformId + (isWindows() ? ".exe" : "");

                String downloadUrl = "https://github.com/EasyTier/EasyTier/releases/download/" + versionTag + "/";

                // Download easytier-core
                progressCallback.accept("Downloading easytier-core " + versionTag + "...");
                Path coreTemp = binDir.resolve(BINARY_NAME + ".new");
                downloadFile(client, downloadUrl + coreAsset, coreTemp);
                makeExecutable(coreTemp);

                // Download easytier-cli
                progressCallback.accept("Downloading easytier-cli " + versionTag + "...");
                Path cliTemp = binDir.resolve(CLI_NAME + ".new");
                downloadFile(client, downloadUrl + cliAsset, cliTemp);
                makeExecutable(cliTemp);

                // Atomic replacement: move .new files over existing ones
                Path corePath = binDir.resolve(BINARY_NAME);
                Path cliPath = binDir.resolve(CLI_NAME);

                if (Files.exists(corePath)) Files.delete(corePath);
                Files.move(coreTemp, corePath);

                if (Files.exists(cliPath)) Files.delete(cliPath);
                Files.move(cliTemp, cliPath);

                // Write version file
                Files.writeString(binDir.resolve(VERSION_FILE), versionTag);
                currentVersion = versionTag;

                progressCallback.accept("Update to " + versionTag + " complete!");
                EasyTierMod.LOGGER.info("[EasyTier] Updated to version {}", versionTag);
                return true;
            } catch (Exception e) {
                EasyTierMod.LOGGER.error("[EasyTier] Update failed: {}", e.getMessage());
                progressCallback.accept("Update failed: " + e.getMessage());
                return false;
            }
        });
    }

    private static void downloadFile(HttpClient client, String url, Path dest) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofMinutes(5))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed with status " + response.statusCode() + " for " + url);
        }

        try (InputStream in = response.body()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Remove all EasyTier binaries and data.
     */
    public static void uninstall() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path easyTierDir = gameDir.resolve("easytier");
        if (Files.exists(easyTierDir)) {
            try (Stream<Path> walk = Files.walk(easyTierDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException ignored) {}
                        });
                EasyTierMod.LOGGER.info("[EasyTier] Uninstalled: {}", easyTierDir);
            } catch (IOException e) {
                EasyTierMod.LOGGER.error("[EasyTier] Failed to uninstall: {}", e.getMessage());
            }
        }
        binDir = null;
        currentVersion = null;
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\": \"";
            start = json.indexOf(search);
        }
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
