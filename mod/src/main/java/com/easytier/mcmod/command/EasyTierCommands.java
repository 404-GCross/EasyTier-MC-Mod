package com.easytier.mcmod.command;

import com.easytier.mcmod.EasyTierMod;
import com.easytier.mcmod.easytier.EasyTierCli;
import com.easytier.mcmod.easytier.NativeLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class EasyTierCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("easytier");

            // /easytier status
            root.then(Commands.literal("status").executes(ctx -> {
                cmd(ctx, "node", "info");
                return 1;
            }));
            // /easytier peers
            root.then(Commands.literal("peers").executes(ctx -> {
                cmd(ctx, "peer", "list");
                return 1;
            }));
            // /easytier routes
            root.then(Commands.literal("routes").executes(ctx -> {
                cmd(ctx, "route", "list");
                return 1;
            }));
            // /easytier stats
            root.then(Commands.literal("stats").executes(ctx -> {
                cmd(ctx, "stats", "show");
                return 1;
            }));
            // /easytier start
            root.then(Commands.literal("start").executes(ctx -> {
                if (!NativeLoader.isInstalled()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§cEasyTier binary not found. Place easytier-core in .minecraft/easytier/bin/"), false);
                    return 0;
                }
                EasyTierMod.startEasyTier();
                ctx.getSource().sendSuccess(() -> Component.literal("§aStarting..."), true);
                return 1;
            }));
            // /easytier stop
            root.then(Commands.literal("stop").executes(ctx -> {
                EasyTierMod.stopEasyTier();
                ctx.getSource().sendSuccess(() -> Component.literal("§aStopped"), true);
                return 1;
            }));
            // /easytier restart
            root.then(Commands.literal("restart").executes(ctx -> {
                var p = EasyTierMod.getEasyTierProcess();
                if (p != null) p.restart(); else EasyTierMod.startEasyTier();
                ctx.getSource().sendSuccess(() -> Component.literal("§aRestarting..."), true);
                return 1;
            }));

            // /easytier connector list|add|remove
            var connector = Commands.literal("connector");
            connector.then(Commands.literal("list").executes(ctx -> { cmd(ctx, "connector", "list"); return 1; }));
            connector.then(Commands.literal("add").then(Commands.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                String url = StringArgumentType.getString(ctx, "url");
                EasyTierCli.execute(EasyTierMod.getConfig(), "connector", "add", url);
                ctx.getSource().sendSuccess(() -> Component.literal("§aAdded: " + url), false);
                return 1;
            })));
            connector.then(Commands.literal("remove").then(Commands.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                String url = StringArgumentType.getString(ctx, "url");
                EasyTierCli.execute(EasyTierMod.getConfig(), "connector", "remove", url);
                ctx.getSource().sendSuccess(() -> Component.literal("§aRemoved: " + url), false);
                return 1;
            })));
            root.then(connector);

            // /easytier version
            root.then(Commands.literal("version").executes(ctx -> {
                String v = NativeLoader.getCurrentVersion();
                ctx.getSource().sendSuccess(() -> Component.literal("EasyTier: " + v), false);
                return 1;
            }));

            // /easytier uninstall
            root.then(Commands.literal("uninstall").executes(ctx -> {
                EasyTierMod.stopEasyTier();
                NativeLoader.uninstall();
                ctx.getSource().sendSuccess(() -> Component.literal("§aUninstalled."), true);
                return 1;
            }));

            // /easytier config
            root.then(Commands.literal("config").executes(ctx -> {
                var c = EasyTierMod.getConfig();
                var sb = new StringBuilder("§6--- Config ---\n");
                sb.append("§7Network: §f").append(c.networkName).append("\n");
                sb.append("§7Hostname: §f").append(c.hostname.isEmpty() ? "(auto)" : c.hostname).append("\n");
                sb.append("§7RPC: §f").append(c.rpcHost).append(":").append(c.rpcPort).append("\n");
                sb.append("§7Version: §f").append(NativeLoader.getCurrentVersion());
                ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                return 1;
            }));

            // /easytier help
            root.then(Commands.literal("help").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("""
                        §6--- EasyTier Commands ---
                        §e/easytier status §7- Show node info
                        §e/easytier peers §7- List peers
                        §e/easytier routes §7- Show routes
                        §e/easytier stats §7- Show statistics
                        §e/easytier config §7- Show config
                        §e/easytier version §7- Show EasyTier version
                        §e/easytier start/stop/restart §7- Manage process
                        §e/easytier connector list|add|remove §7- Manage peers
                        §e/easytier uninstall §7- Remove binaries"""), false);
                return 1;
            }));

            dispatcher.register(root);
        });
    }

    private static void cmd(net.minecraft.commands.CommandSourceStack ctx, String... args) {
        var config = EasyTierMod.getConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("§6Fetching..."), false);
        EasyTierCli.executeJson(config, args).thenAccept(json ->
                ctx.getSource().sendSuccess(() -> Component.literal(formatJson(json)), false)
        ).exceptionally(e -> {
            ctx.getSource().sendSuccess(() -> Component.literal("§c" + e.getMessage()), false);
            return null;
        });
    }

    private static String formatJson(com.google.gson.JsonElement json) {
        if (json == null) return "No data";
        try { return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json); }
        catch (Exception e) { return json.toString(); }
    }
}
