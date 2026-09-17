package net.nextfur.fws.bukkit.modules.action;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ActionCommand implements CommandExecutor, TabCompleter {
    private final FurWatchBukkit plugin;
    private final ActionManager actionManager;

    public ActionCommand(FurWatchBukkit plugin, ActionManager actionManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("furwatch.action") || sender.hasPermission("furwatch.admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores no jogo podem executar ações de roleplay!");
            return true;
        }

        Player player = (Player) sender;

        if (!hasPermission(player)) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando de ação.");
            return true;
        }

        if (!actionManager.isEnabled()) {
            player.sendMessage(ActionManager.colorize(actionManager.getDisabledMessage()));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ActionManager.colorize(actionManager.getUsageMessage()));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Subcommand: reload
        if (sub.equals("reload")) {
            if (!player.hasPermission("furwatch.admin") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para recarregar as configurações de ação.");
                return true;
            }

            try {
                plugin._getConfig().reload();
                actionManager.reload(plugin._getConfig());
                player.sendMessage(ChatColor.GREEN + "[FurWatch] Módulo de ações de roleplay recarregado com sucesso!");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Erro ao recarregar ações: " + e.getMessage());
            }
            return true;
        }

        // Subcommand: lista / list / help / ajuda
        if (sub.equals("lista") || sub.equals("list") || sub.equals("ajuda") || sub.equals("help")) {
            sendPresetList(player, label);
            return true;
        }

        // Check cooldown
        if (actionManager.isOnCooldown(player)) {
            long remaining = actionManager.getRemainingCooldown(player);
            String msg = actionManager.getCooldownMessage().replace("%seconds%", String.valueOf(remaining));
            player.sendMessage(ActionManager.colorize(msg));
            return true;
        }

        String rawInput = String.join(" ", args);

        // Try matching presets (exact ID, prefix with target, or regex)
        ActionPreset.MatchResult match = actionManager.matchPreset(rawInput);
        String finalMessage;

        if (match != null) {
            finalMessage = match.format(player.getName());
        } else {
            finalMessage = actionManager.formatCustomAction(player, rawInput);
        }

        // Broadcast to nearby players via ActionBar
        actionManager.broadcastAction(player, finalMessage);
        actionManager.applyCooldown(player);

        return true;
    }

    private void sendPresetList(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "=== [ Presets de Ação de Roleplay ] ===");
        player.sendMessage(ChatColor.GRAY + "Dica: Você também pode digitar qualquer ação livre! Ex: "
                + ChatColor.YELLOW + "/" + label + " olha em volta admirado");

        for (ActionPreset preset : actionManager.getPresets()) {
            player.sendMessage(ChatColor.YELLOW + "/" + label + " " + preset.getId() + ChatColor.WHITE + " [alvo]"
                    + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + preset.getDescription());
        }
        player.sendMessage(ChatColor.GOLD + "========================================");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player) || !hasPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (ActionPreset preset : actionManager.getPresets()) {
                suggestions.add(preset.getId());
            }
            suggestions.add("lista");

            if (sender.hasPermission("furwatch.admin") || sender.isOp()) {
                suggestions.add("reload");
            }

            String prefix = args[0].toLowerCase(Locale.ROOT);
            return suggestions.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (!first.equals("lista") && !first.equals("list") && !first.equals("reload")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
