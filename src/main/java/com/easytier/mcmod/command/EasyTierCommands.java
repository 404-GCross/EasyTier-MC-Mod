package com.easytier.mcmod.command;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.easytier.EasyTierCli;
import com.easytier.mcmod.easytier.NativeLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Registers all /easytier commands.
 */
public class EasyTierCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("easytier");

            // /easytier status
            root.then(Commands.literal("status").executes(ctx -> {
                var config = EasyTierMod.getConfig();
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Fetching status..."), false);
                EasyTierCli.executeJson(config, "node", "info").thenAccept(json -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(formatJson(json)), false);
                }).exceptionally(e -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                    return null;
                });
                return 1;
            }));

            // /easytier peers
            root.then(Commands.literal("peers").executes(ctx -> {
                var config = EasyTierMod.getConfig();
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Fetching peers..."), false);
                EasyTierCli.executeJson(config, "peer", "list").thenAccept(json -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(formatJson(json)), false);
                }).exceptionally(e -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                    return null;
                });
                return 1;
            }));

            // /easytier routes
            root.then(Commands.literal("routes").executes(ctx -> {
                var config = EasyTierMod.getConfig();
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Fetching routes..."), false);
                EasyTierCli.executeJson(config, "route", "list").thenAccept(json -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(formatJson(json)), false);
                }).exceptionally(e -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                    return null;
                });
                return 1;
            }));

            // /easytier stats
            root.then(Commands.literal("stats").executes(ctx -> {
                var config = EasyTierMod.getConfig();
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Fetching stats..."), false);
                EasyTierCli.executeJson(config, "stats", "show").thenAccept(json -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(formatJson(json)), false);
                }).exceptionally(e -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                    return null;
                });
                return 1;
            }));

            // /easytier start
            root.then(Commands.literal("start").executes(ctx -> {
                EasyTierMod.startEasyTier();
                ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Starting..."), true);
                return 1;
            }));

            // /easytier stop
            root.then(Commands.literal("stop").executes(ctx -> {
                EasyTierMod.stopEasyTier();
                ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Stopped"), true);
                return 1;
            }));

            // /easytier restart
            root.then(Commands.literal("restart").executes(ctx -> {
                var proc = EasyTierMod.getEasyTierProcess();
                if (proc != null) {
                    proc.restart();
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Restarting..."), true);
                } else {
                    EasyTierMod.startEasyTier();
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Starting..."), true);
                }
                return 1;
            }));

            // /easytier connector
            var connector = Commands.literal("connector");
            connector.then(Commands.literal("list").executes(ctx -> {
                var config = EasyTierMod.getConfig();
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Fetching connectors..."), false);
                EasyTierCli.executeJson(config, "connector", "list").thenAccept(json -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(formatJson(json)), false);
                }).exceptionally(e -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                    return null;
                });
                return 1;
            }));
            connector.then(Commands.literal("add")
                    .then(Commands.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                        var config = EasyTierMod.getConfig();
                        String url = StringArgumentType.getString(ctx, "url");
                        ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Adding connector: " + url), false);
                        EasyTierCli.execute(config, "connector", "add", url).thenAccept(result -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Connector added"), false);
                        }).exceptionally(e -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                            return null;
                        });
                        return 1;
                    })));
            connector.then(Commands.literal("remove")
                    .then(Commands.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                        var config = EasyTierMod.getConfig();
                        String url = StringArgumentType.getString(ctx, "url");
                        ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Removing connector: " + url), false);
                        EasyTierCli.execute(config, "connector", "remove", url).thenAccept(result -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Connector removed"), false);
                        }).exceptionally(e -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§cError: " + e.getMessage()), false);
                            return null;
                        });
                        return 1;
                    })));
            root.then(connector);

            // /easytier install - extract bundled binaries
            root.then(Commands.literal("install").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Installing..."), false);
                try {
                    NativeLoader.getBinDir();
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Installation complete! Platform: " + NativeLoader.getPlatformId()), true);
                } catch (Exception e) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§c[EasyTier] Install failed: " + e.getMessage()), false);
                }
                return 1;
            }));

            // /easytier update [version] - download update from GitHub
            root.then(Commands.literal("update")
                    .then(Commands.argument("version", StringArgumentType.word()).executes(ctx -> {
                        String version = StringArgumentType.getString(ctx, "version");
                        ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Downloading update " + version + "..."), false);
                        NativeLoader.downloadUpdate(version, msg -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§e[EasyTier] " + msg), false);
                        }).thenAccept(success -> {
                            if (success) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Update complete! Use /easytier restart to apply."), false);
                            } else {
                                ctx.getSource().sendSuccess(() -> Component.literal("§c[EasyTier] Update failed. Check logs."), false);
                            }
                        });
                        return 1;
                    })));

            // /easytier check-update
            root.then(Commands.literal("check-update").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Checking for updates..."), false);
                NativeLoader.checkForUpdate().thenAccept(latest -> {
                    if (latest != null) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§e[EasyTier] New version available: " + latest + " (current: " + NativeLoader.getCurrentVersion() + ")"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Up to date (version: " + NativeLoader.getCurrentVersion() + ")"), false);
                    }
                });
                return 1;
            }));

            // /easytier uninstall - remove all EasyTier binaries
            root.then(Commands.literal("uninstall").executes(ctx -> {
                EasyTierMod.stopEasyTier();
                NativeLoader.uninstall();
                ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Uninstalled. All binaries removed."), true);
                return 1;
            }));

            // /easytier config (shows current config summary)
            root.then(Commands.literal("config").executes(ctx -> {
                var c = EasyTierMod.getConfig();
                StringBuilder sb = new StringBuilder();
                sb.append("§6--- EasyTier Config ---\n");
                sb.append("§7Network: §f").append(c.networkName).append("\n");
                sb.append("§7RPC: §f").append(c.rpcHost).append(":").append(c.rpcPort).append("\n");
                sb.append("§7Protocol: §f").append(c.defaultProtocol).append("\n");
                sb.append("§7Auto-start: §f").append(c.autoStart).append("\n");
                sb.append("§7Encryption: §f").append(c.enableEncryption).append("\n");
                sb.append("§7IPv6: §f").append(c.enableIpv6).append("\n");
                sb.append("§7KCP: §f").append(c.enableKcpProxy).append("  §7QUIC: §f").append(c.enableQuicProxy).append("\n");
                sb.append("§7Version: §f").append(NativeLoader.getCurrentVersion());
                ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                return 1;
            }));

            // /easytier help
            root.then(Commands.literal("help").executes(ctx -> {
                String help = """
                        §6--- EasyTier Commands ---
                        §e/easytier status §7- Show local node info
                        §e/easytier peers §7- List connected peers
                        §e/easytier routes §7- Show route table
                        §e/easytier stats §7- Show statistics
                        §e/easytier config §7- Show current config
                        §e/easytier start §7- Start EasyTier process
                        §e/easytier stop §7- Stop EasyTier process
                        §e/easytier restart §7- Restart EasyTier process
                        §e/easytier connector list|add|remove §7- Manage connectors
                        §e/easytier install §7- Extract bundled binaries
                        §e/easytier update <version> §7- Download update
                        §e/easytier check-update §7- Check for updates
                        §e/easytier uninstall §7- Remove all EasyTier binaries""";
                ctx.getSource().sendSuccess(() -> Component.literal(help), false);
                return 1;
            }));

            dispatcher.register(root);
        });
    }

    private static String formatJson(com.google.gson.JsonElement json) {
        if (json == null) return "No data";
        try {
            return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
        } catch (Exception e) {
            return json.toString();
        }
    }
}
