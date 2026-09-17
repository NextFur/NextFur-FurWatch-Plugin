package net.nextfur.fws.bukkit.modules.death;

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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DeathCommand implements CommandExecutor, TabCompleter {
    private final FurWatchBukkit plugin;
    private final DeathManager deathManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public DeathCommand(FurWatchBukkit plugin, DeathManager deathManager) {
        this.plugin = plugin;
        this.deathManager = deathManager;
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.isOp() || sender.hasPermission(deathManager.getPermission()) || sender.hasPermission("furwatch.admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para executar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "top":
                handleTop(sender, args);
                break;
            case "check":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso correto: /" + label + " check <jogador>");
                    return true;
                }
                handleCheck(sender, args[1]);
                break;
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso correto: /" + label + " set <jogador> <quantidade>");
                    return true;
                }
                handleSet(sender, args[1], args[2]);
                break;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso correto: /" + label + " add <jogador> [quantidade]");
                    return true;
                }
                String amountStr = args.length >= 3 ? args[2] : "1";
                handleAdd(sender, args[1], amountStr);
                break;
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso correto: /" + label + " reset <jogador>");
                    return true;
                }
                handleReset(sender, args[1]);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                // Treat /deathcount <player> as /deathcount check <player>
                handleCheck(sender, args[0]);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== [ FurWatch - Deathcount ;3 ] ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " check <jogador>" + ChatColor.GRAY + " - Consulta histórico e total de mortes");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " top [quantidade]" + ChatColor.GRAY + " - Ranking de jogadores com mais mortes");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <jogador> <qtd>" + ChatColor.GRAY + " - Define a contagem de mortes de um jogador");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " add <jogador> [qtd]" + ChatColor.GRAY + " - Incrementa a contagem de mortes");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reset <jogador>" + ChatColor.GRAY + " - Zera as mortes de um jogador");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Recarrega a configuração e dados do módulo");
    }

    private void handleCheck(CommandSender sender, String targetName) {
        PlayerDeathData data = deathManager.getData(targetName);

        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Nenhum registro de mortes encontrado para o jogador: " + ChatColor.YELLOW + targetName);
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Estatísticas de Morte: " + ChatColor.YELLOW + data.getLastKnownName() + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + data.getUuid().toString());
        sender.sendMessage(ChatColor.GRAY + "Total de Mortes: " + ChatColor.RED + data.getDeathCount());

        if (data.getLastDeathTimestamp() > 0) {
            String dateFormatted = dateFormat.format(new Date(data.getLastDeathTimestamp()));
            sender.sendMessage(ChatColor.GRAY + "Última Morte: " + ChatColor.WHITE + dateFormatted);
            sender.sendMessage(ChatColor.GRAY + "Causa: " + ChatColor.WHITE + data.getLastDeathMessage());
            sender.sendMessage(ChatColor.GRAY + "Localização: " + ChatColor.WHITE + data.getLastLocation());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Última Morte: " + ChatColor.DARK_GRAY + "Nenhuma morte registrada ainda.");
        }
    }

    private void handleTop(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {}
        }

        List<PlayerDeathData> top = deathManager.getTopDeaths(limit);

        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhuma morte registrada no servidor até o momento.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Top " + top.size() + " Jogadores com Mais Mortes ===");
        int rank = 1;
        for (PlayerDeathData entry : top) {
            sender.sendMessage(ChatColor.GOLD + "#" + rank + " " + ChatColor.YELLOW + entry.getLastKnownName()
                    + ChatColor.DARK_GRAY + " - " + ChatColor.RED + entry.getDeathCount() + " mortes");
            rank++;
        }
    }

    private void handleSet(CommandSender sender, String targetName, String amountStr) {
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantidade inválida. Digite um número inteiro maior ou igual a 0.");
            return;
        }

        UUID uuid = getUuidOrResolve(targetName);
        deathManager.setDeathCount(uuid, targetName, amount);
        sender.sendMessage(ChatColor.GREEN + "Contagem de mortes de " + ChatColor.YELLOW + targetName
                + ChatColor.GREEN + " alterada para " + ChatColor.GOLD + amount + ChatColor.GREEN + ".");
        plugin._getLogger().info("Staff " + sender.getName() + " definiu mortes de " + targetName + " para " + amount);
    }

    private void handleAdd(CommandSender sender, String targetName, String amountStr) {
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantidade inválida. Digite um número inteiro positivo.");
            return;
        }

        UUID uuid = getUuidOrResolve(targetName);
        deathManager.addDeathCount(uuid, targetName, amount);
        PlayerDeathData data = deathManager.getData(uuid);
        int total = data != null ? data.getDeathCount() : amount;

        sender.sendMessage(ChatColor.GREEN + "Adicionadas " + ChatColor.GOLD + amount + ChatColor.GREEN + " mortes para "
                + ChatColor.YELLOW + targetName + ChatColor.GREEN + ". Novo total: " + ChatColor.RED + total + ChatColor.GREEN + ".");
        plugin._getLogger().info("Staff " + sender.getName() + " adicionou " + amount + " mortes para " + targetName);
    }

    private void handleReset(CommandSender sender, String targetName) {
        PlayerDeathData data = deathManager.getData(targetName);
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Nenhum dado encontrado para o jogador: " + targetName);
            return;
        }

        deathManager.resetDeathCount(data.getUuid(), data.getLastKnownName());
        sender.sendMessage(ChatColor.GREEN + "Contador de mortes de " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " foi zerado com sucesso.");
        plugin._getLogger().info("Staff " + sender.getName() + " zerou as mortes de " + targetName);
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin._getConfig().reload();
            deathManager.reload(plugin._getConfig());
            sender.sendMessage(ChatColor.GREEN + "[FurWatch] Configurações e deathcount recarregadas com sucesso!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao recarregar: " + e.getMessage());
        }
    }

    private UUID getUuidOrResolve(String targetName) {
        Player player = Bukkit.getPlayerExact(targetName);
        if (player != null) {
            return player.getUniqueId();
        }
        PlayerDeathData data = deathManager.getData(targetName);
        if (data != null) {
            return data.getUuid();
        }
        // Fallback for offline player
        //noinspection deprecation
        return Bukkit.getOfflinePlayer(targetName).getUniqueId();
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("help", "check", "top", "set", "add", "reset", "reload");
            String prefix = args[0].toLowerCase();
            return subs.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("check") || sub.equals("set") || sub.equals("add") || sub.equals("reset")) {
                String prefix = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
