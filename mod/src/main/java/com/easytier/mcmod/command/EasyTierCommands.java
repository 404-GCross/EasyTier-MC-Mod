package com.easytier.mcmod.command;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.easytier.EasyTierCli;
import com.easytier.mcmod.easytier.NativeLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

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
                if (!NativeLoader.isInstalled()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§c[EasyTier] EasyTier is not installed. Use /easytier download <version> first."), false);
                    return 0;
                }
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

            // ============ Download & Update commands ============

            // /easytier versions — list available versions
            root.then(Commands.literal("versions").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Fetching versions..."), false);
                NativeLoader.fetchAvailableVersions().thenAccept(versions -> {
                    if (versions.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§cNo versions found. Check API mirror settings."), false);
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("§6--- Available Versions ---\n");
                    String current = NativeLoader.getCurrentVersion();
                    for (int i = 0; i < Math.min(versions.size(), 15); i++) {
                        var v = versions.get(i);
                        String marker = v.tag.equals(current) ? " §a[installed]" : "";
                        String pre = v.prerelease ? " §7[pre]" : "";
                        sb.append("§e").append(v.tag).append(pre).append(marker)
                                .append(" §7- ").append(v.name)
                                .append(" (").append(v.publishedAt).append(")\n");
                    }
                    if (versions.size() > 15) {
                        sb.append("§7... and ").append(versions.size() - 15).append(" more versions\n");
                    }
                    sb.append("\n§7Use §e/easytier download <version>§7 to install");
                    ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }).exceptionally(e -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cFailed to fetch versions: " + e.getMessage()), false);
                    return null;
                });
                return 1;
            }));

            // /easytier download <version> — download and install specific version
            root.then(Commands.literal("download")
                    .then(Commands.argument("version", StringArgumentType.word()).executes(ctx -> {
                        String version = StringArgumentType.getString(ctx, "version");
                        ctx.getSource().sendSuccess(() -> Component.literal("§6[EasyTier] Downloading " + version + "..."), false);
                        NativeLoader.downloadUpdate(version, msg -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§e[EasyTier] " + msg), false);
                        }).thenAccept(success -> {
                            if (success) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Download complete! Use /easytier start to launch."), false);
                            } else {
                                ctx.getSource().sendSuccess(() -> Component.literal("§c[EasyTier] Download failed. Check logs or try a different mirror."), false);
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
                        ctx.getSource().sendSuccess(() -> Component.literal("§7Use §e/easytier download " + latest + "§7 to install"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Up to date (version: " + NativeLoader.getCurrentVersion() + ")"), false);
                    }
                });
                return 1;
            }));

            // ============ Mirror management ============

            var mirror = Commands.literal("mirror");

            // /easytier mirror show — show current mirror config
            mirror.then(Commands.literal("show").executes(ctx -> {
                var c = EasyTierMod.getConfig();
                StringBuilder sb = new StringBuilder();
                sb.append("§6--- Mirror Settings ---\n");
                sb.append("§7API Mirror: §f").append(c.apiMirror.isEmpty() ? "https://api.github.com (default)" : c.apiMirror).append("\n");
                sb.append("§7Download Mirror: §f").append(c.downloadMirror.isEmpty() ? "GitHub Releases (default)" : c.downloadMirror).append("\n");
                sb.append("\n§7Use §e/easytier mirror api <url>§7 or §e/easytier mirror download <url>\n");
                sb.append("§7Use §e/easytier mirror api reset§7 to restore defaults");
                ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                return 1;
            }));
            root.then(mirror);

            // /easytier mirror api <url> — set API mirror
            mirror.then(Commands.literal("api")
                    .then(Commands.literal("reset").executes(ctx -> {
                        EasyTierMod.getConfig().apiMirror = "";
                        EasyTierMod.getConfig().save(FabricLoader.getInstance().getConfigDir());
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] API mirror reset to default (api.github.com)"), false);
                        return 1;
                    }))
                    .then(Commands.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                        String url = StringArgumentType.getString(ctx, "url");
                        EasyTierMod.getConfig().apiMirror = url;
                        EasyTierMod.getConfig().save(FabricLoader.getInstance().getConfigDir());
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] API mirror set to: " + url), false);
                        return 1;
                    })));

            // /easytier mirror download <url> — set download mirror
            mirror.then(Commands.literal("download")
                    .then(Commands.literal("reset").executes(ctx -> {
                        EasyTierMod.getConfig().downloadMirror = "";
                        EasyTierMod.getConfig().save(FabricLoader.getInstance().getConfigDir());
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Download mirror reset to default (GitHub Releases)"), false);
                        return 1;
                    }))
                    .then(Commands.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                        String url = StringArgumentType.getString(ctx, "url");
                        EasyTierMod.getConfig().downloadMirror = url;
                        EasyTierMod.getConfig().save(FabricLoader.getInstance().getConfigDir());
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Download mirror set to: " + url), false);
                        return 1;
                    })));

            // ============ Uninstall ============

            // /easytier uninstall
            root.then(Commands.literal("uninstall").executes(ctx -> {
                EasyTierMod.stopEasyTier();
                NativeLoader.uninstall();
                ctx.getSource().sendSuccess(() -> Component.literal("§a[EasyTier] Uninstalled. All binaries removed."), true);
                return 1;
            }));

            // /easytier config
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
                sb.append("§7API Mirror: §f").append(c.apiMirror.isEmpty() ? "(default)" : c.apiMirror).append("\n");
                sb.append("§7Download Mirror: §f").append(c.downloadMirror.isEmpty() ? "(default)" : c.downloadMirror).append("\n");
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
                        §e/easytier start/stop/restart §7- Manage EasyTier process
                        §e/easytier connector list|add|remove §7- Manage connectors
                        §e/easytier versions §7- List available EasyTier versions
                        §e/easytier download <version> §7- Download & install EasyTier
                        §e/easytier check-update §7- Check for latest version
                        §e/easytier mirror show|api|download §7- Configure mirrors
                        §e/easytier uninstall §7- Remove EasyTier completely""";
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
