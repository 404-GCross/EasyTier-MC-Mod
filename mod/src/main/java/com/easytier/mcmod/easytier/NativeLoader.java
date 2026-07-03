package com.easytier.mcmod.easytier;

import com.easytier.mcmod.EasyTierMod;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Handles native EasyTier binary installation and updates.
 * On first run, auto-downloads from GitHub (or configured mirror).
 * Supports version listing, selection, and custom download mirrors.
 */
public class NativeLoader {
    private static final String BINARY_NAME = isWindows() ? "easytier-core.exe" : "easytier-core";
    private static final String CLI_NAME = isWindows() ? "easytier-cli.exe" : "easytier-cli";
    private static final String VERSION_FILE = "version.txt";

    private static final String GITHUB_API = "https://api.github.com/repos/EasyTier/EasyTier/releases";
    private static final String GITHUB_LATEST = "https://api.github.com/repos/EasyTier/EasyTier/releases/latest";

    private static Path binDir = null;
    private static String currentVersion = null;

    /**
     * Version info for selection UI.
     */
    public static class VersionInfo {
        public final String tag;
        public final String name;
        public final boolean prerelease;
        public final String publishedAt;

        public VersionInfo(String tag, String name, boolean prerelease, String publishedAt) {
            this.tag = tag;
            this.name = name;
            this.prerelease = prerelease;
            this.publishedAt = publishedAt;
        }

        @Override
        public String toString() {
            return (prerelease ? "[pre] " : "") + tag + " - " + name;
        }
    }

    /**
     * Get or initialize the binary directory. Auto-downloads on first run.
     */
    public static Path getBinDir() throws IOException {
        if (binDir != null) return binDir;

        Path gameDir = FabricLoader.getInstance().getGameDir();
        binDir = gameDir.resolve("easytier").resolve("bin");
        Files.createDirectories(binDir);

        Path corePath = binDir.resolve(BINARY_NAME);
        Path versionPath = binDir.resolve(VERSION_FILE);

        if (!Files.exists(corePath)) {
            // Try bundled first, then schedule auto-download
            EasyTierMod.LOGGER.info("[EasyTier] First run — checking bundled binaries...");
            boolean extracted = extractBundledBinary("easytier-core", BINARY_NAME, corePath);
            if (extracted) {
                extractBundledBinary("easytier-cli", CLI_NAME, binDir.resolve(CLI_NAME));
                Files.writeString(versionPath, "bundled");
                currentVersion = "bundled";
            } else {
                EasyTierMod.LOGGER.info("[EasyTier] No bundled binary — will auto-download on first start");
                currentVersion = "none";
            }
        } else {
            makeExecutable(corePath);
            makeExecutable(binDir.resolve(CLI_NAME));
            if (Files.exists(versionPath)) {
                currentVersion = Files.readString(versionPath).trim();
            }
        }

        return binDir;
    }

    public static boolean isInstalled() {
        try {
            Path binDir = getBinDir();
            return Files.exists(binDir.resolve(BINARY_NAME));
        } catch (IOException e) {
            return false;
        }
    }

    public static String getCurrentVersion() {
        if (currentVersion != null && !currentVersion.equals("none")) return currentVersion;
        try {
            Path versionPath = getBinDir().resolve(VERSION_FILE);
            if (Files.exists(versionPath)) {
                currentVersion = Files.readString(versionPath).trim();
                return currentVersion;
            }
        } catch (IOException ignored) {}
        return "not installed";
    }

    /**
     * Fetch available versions from GitHub API (or mirror).
     * Returns list sorted newest-first.
     */
    public static CompletableFuture<List<VersionInfo>> fetchAvailableVersions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String mirror = EasyTierMod.getConfig() != null
                        ? EasyTierMod.getConfig().apiMirror
                        : "";
                String apiUrl = mirror.isEmpty() ? GITHUB_API
                        : mirror.endsWith("/") ? mirror + "repos/EasyTier/EasyTier/releases"
                        : mirror + "/repos/EasyTier/EasyTier/releases";

                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .timeout(java.time.Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("API returned " + response.statusCode());
                }

                JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
                List<VersionInfo> versions = new ArrayList<>();

                for (JsonElement el : releases) {
                    JsonObject obj = el.getAsJsonObject();
                    String tag = obj.get("tag_name").getAsString();
                    String name = obj.has("name") && !obj.get("name").isJsonNull()
                            ? obj.get("name").getAsString() : tag;
                    boolean prerelease = obj.has("prerelease") && obj.get("prerelease").getAsBoolean();
                    String publishedAt = obj.has("published_at") && !obj.get("published_at").isJsonNull()
                            ? obj.get("published_at").getAsString().substring(0, 10) : "";
                    versions.add(new VersionInfo(tag, name, prerelease, publishedAt));
                }

                return versions;
            } catch (Exception e) {
                EasyTierMod.LOGGER.error("[EasyTier] Failed to fetch versions: {}", e.getMessage());
                throw new RuntimeException("Failed to fetch versions: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Check for newer version (latest only).
     */
    public static CompletableFuture<String> checkForUpdate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String mirror = EasyTierMod.getConfig() != null
                        ? EasyTierMod.getConfig().apiMirror : "";
                String apiUrl = mirror.isEmpty() ? GITHUB_LATEST
                        : mirror.endsWith("/") ? mirror + "repos/EasyTier/EasyTier/releases/latest"
                        : mirror + "/repos/EasyTier/EasyTier/releases/latest";

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/vnd.github+json")
                        .timeout(java.time.Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return null;

                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                String tagName = obj.get("tag_name").getAsString();
                String current = getCurrentVersion();
                return tagName.equals(current) ? null : tagName;
            } catch (Exception e) {
                EasyTierMod.LOGGER.warn("[EasyTier] Failed to check updates: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * Download and install EasyTier binaries.
     * @param versionTag e.g. "v2.6.4"
     * @param progressCallback receives progress messages
     * @return CompletableFuture that completes with true on success
     */
    public static CompletableFuture<Boolean> downloadUpdate(String versionTag,
                                                             Consumer<String> progressCallback) {
        String mirror = EasyTierMod.getConfig() != null
                ? EasyTierMod.getConfig().downloadMirror : "";
        return downloadUpdate(versionTag, progressCallback, mirror);
    }

    /**
     * Download from a specific mirror URL (or GitHub if empty).
     */
    public static CompletableFuture<Boolean> downloadUpdate(String versionTag,
                                                             Consumer<String> progressCallback,
                                                             String mirrorUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path binDir = getBinDir();
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(java.time.Duration.ofSeconds(15))
                        .build();

                String platformId = getPlatformId();
                String baseUrl;

                if (mirrorUrl != null && !mirrorUrl.isEmpty()) {
                    // Custom mirror: https://example.com/path/
                    String base = mirrorUrl.endsWith("/") ? mirrorUrl : mirrorUrl + "/";
                    baseUrl = base + versionTag + "/";
                } else {
                    // Default GitHub
                    baseUrl = "https://github.com/EasyTier/EasyTier/releases/download/" + versionTag + "/";
                }

                String coreSuffix = isWindows() ? ".exe" : "";
                String coreAsset = "easytier-core-" + platformId + coreSuffix;
                String cliAsset = "easytier-cli-" + platformId + coreSuffix;

                // Download easytier-core
                progressCallback.accept("Downloading easytier-core " + versionTag + "...");
                Path coreTemp = binDir.resolve(BINARY_NAME + ".new");
                downloadFile(client, baseUrl + coreAsset, coreTemp);
                makeExecutable(coreTemp);

                // Download easytier-cli
                progressCallback.accept("Downloading easytier-cli " + versionTag + "...");
                Path cliTemp = binDir.resolve(CLI_NAME + ".new");
                downloadFile(client, baseUrl + cliAsset, cliTemp);
                makeExecutable(cliTemp);

                // Atomic replacement
                Path corePath = binDir.resolve(BINARY_NAME);
                Path cliPath = binDir.resolve(CLI_NAME);
                if (Files.exists(corePath)) Files.delete(corePath);
                Files.move(coreTemp, corePath);
                if (Files.exists(cliPath)) Files.delete(cliPath);
                Files.move(cliTemp, cliPath);

                // Write version file
                Files.writeString(binDir.resolve(VERSION_FILE), versionTag);
                currentVersion = versionTag;

                progressCallback.accept("Update to " + versionTag + " complete! Restart EasyTier to apply.");
                EasyTierMod.LOGGER.info("[EasyTier] Updated to version {}", versionTag);
                return true;
            } catch (IOException | InterruptedException e) {
                EasyTierMod.LOGGER.error("[EasyTier] Update failed: {}", e.getMessage());
                progressCallback.accept("Update failed: " + e.getMessage());
                return false;
            }
        });
    }

    private static void downloadFile(HttpClient client, String url, Path dest)
            throws IOException, InterruptedException {
        EasyTierMod.LOGGER.debug("[EasyTier] Downloading: {}", url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofMinutes(5))
                .build();

        HttpResponse<InputStream> response = client.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed (HTTP " + response.statusCode() + "): " + url);
        }

        try (InputStream in = response.body()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Extract a bundled native binary from the JAR resources.
     * @return true if extracted successfully
     */
    private static boolean extractBundledBinary(String binaryName, String targetName, Path targetPath) throws IOException {
        String resourcePath = "/assets/easytier-mcmod/natives/" + getPlatformId() + "/" + binaryName;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return false;
            }
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            makeExecutable(targetPath);
            EasyTierMod.LOGGER.info("[EasyTier] Extracted bundled: {}", targetPath);
            return true;
        }
    }

    /**
     * Get the platform identifier for native binary selection.
     */
    public static String getPlatformId() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String osPart = os.contains("win") ? "windows"
                : os.contains("mac") || os.contains("darwin") ? "macos"
                : os.contains("linux") ? "linux"
                : os.contains("freebsd") ? "freebsd"
                : "linux";

        String archPart = arch.contains("amd64") || arch.contains("x86_64") ? "x86_64"
                : arch.contains("aarch64") || arch.contains("arm64") ? "aarch64"
                : arch.contains("arm") ? "arm"
                : arch.contains("mips") ? "mips"
                : "x86_64";

        return osPart + "-" + archPart;
    }

    private static void makeExecutable(Path path) {
        if (!isWindows() && Files.exists(path)) {
            try {
                Files.setPosixFilePermissions(path, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
                ));
            } catch (IOException ignored) {}
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Remove all EasyTier binaries and data.
     */
    public static void uninstall() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path easyTierDir = gameDir.resolve("easytier");
        if (Files.exists(easyTierDir)) {
            try (Stream<Path> walk = Files.walk(easyTierDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
                EasyTierMod.LOGGER.info("[EasyTier] Uninstalled: {}", easyTierDir);
            } catch (IOException ignored) {}
        }
        binDir = null;
        currentVersion = null;
    }
}
