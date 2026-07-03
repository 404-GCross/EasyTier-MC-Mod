package com.easytier.mcmod.easytier;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Java wrapper for executing easytier-cli commands asynchronously.
 * Uses ProcessBuilder to run CLI and parse JSON output.
 */
public class EasyTierCli {
    private static final Gson GSON = new Gson();
    private static final long TIMEOUT_SECONDS = 10;

    /**
     * Execute an easytier-cli command and return stdout.
     */
    public static CompletableFuture<String> execute(ModConfig config, String... args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!NativeLoader.isInstalled()) {
                    throw new IOException("EasyTier is not installed. Use /easytier download <version> first.");
                }
                Path binDir = NativeLoader.getBinDir();
                String cliName = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "easytier-cli.exe" : "easytier-cli";
                Path cliPath = binDir.resolve(cliName);

                List<String> command = new ArrayList<>();
                command.add(cliPath.toAbsolutePath().toString());
                command.add("-p");
                command.add(config.rpcHost + ":" + config.rpcPort);
                command.add("-o");
                command.add("json");
                for (String arg : args) {
                    command.add(arg);
                }

                EasyTierMod.LOGGER.debug("[EasyTier] CLI: {}", String.join(" ", command));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new IOException("Command timed out: " + String.join(" ", args));
                }

                if (process.exitValue() != 0) {
                    throw new IOException("Command failed (exit " + process.exitValue() + "): " + output.toString().trim());
                }

                return output.toString().trim();
            } catch (Exception e) {
                EasyTierMod.LOGGER.error("[EasyTier] CLI error: {}", e.getMessage());
                throw new RuntimeException("EasyTier CLI error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Execute a CLI command and parse the JSON response.
     */
    public static CompletableFuture<JsonElement> executeJson(ModConfig config, String... args) {
        return execute(config, args).thenApply(result -> {
            if (result.isEmpty()) {
                return new JsonObject();
            }
            try {
                return JsonParser.parseString(result);
            } catch (Exception e) {
                // If not valid JSON, wrap in an object with raw output
                JsonObject obj = new JsonObject();
                obj.addProperty("raw_output", result);
                return obj;
            }
        });
    }

    /**
     * Execute a CLI command and return the result as a parsed object of the given type.
     */
    public static <T> CompletableFuture<T> executeAndParse(ModConfig config, Class<T> type, String... args) {
        return executeJson(config, args).thenApply(json -> {
            try {
                return GSON.fromJson(json, type);
            } catch (Exception e) {
                EasyTierMod.LOGGER.error("[EasyTier] Failed to parse CLI output as {}: {}", type.getSimpleName(), e.getMessage());
                return null;
            }
        });
    }
}
