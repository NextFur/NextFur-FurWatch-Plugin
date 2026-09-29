package net.nextfur.fws.bukkit.modules.guard;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GuardCommand implements CommandExecutor, TabCompleter {
    private final FurWatchBukkit plugin;
    private final GuardManager guardManager;

    public GuardCommand(FurWatchBukkit plugin, GuardManager guardManager) {
        this.plugin = plugin;
        this.guardManager = guardManager;
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("furwatch.admin") || sender.hasPermission("furwatch.guard.command");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "wand":
                handleWand(sender);
                break;
            case "pos1":
                handlePos(sender, 1, args);
                break;
            case "pos2":
                handlePos(sender, 2, args);
                break;
            case "expand":
                handleExpand(sender, args);
                break;
            case "create":
                handleCreate(sender, args);
                break;
            case "setregion":
                handleSetRegion(sender, args);
                break;
            case "region":
            case "type":
                handleType(sender, args);
                break;
            case "delete":
            case "remove":
                handleDelete(sender, args);
                break;
            case "flag":
                handleFlag(sender, args);
                break;
            case "message":
            case "msg":
                handleMessage(sender, args);
                break;
            case "show":
            case "preview":
                handleShow(sender, args);
                break;
            case "tp":
            case "teleport":
                handleTeleport(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Subcomando desconhecido. Digite /" + label + " help para ajuda.");
                break;
        }

        return true;
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }

        player.getInventory().addItem(guardManager.createWandItem());
        player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Você recebeu a ferramenta " + ChatColor.YELLOW + "FurGuard Wand" + ChatColor.GREEN + "!");
        player.sendMessage(ChatColor.GRAY + "Use o botão esquerdo para pos1 e botão direito para pos2.");
    }

    private void handlePos(CommandSender sender, int point, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }

        GuardSelection sel = guardManager.getOrCreateSelection(player.getUniqueId());
        Location loc;

        if (args.length >= 4) {
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                loc = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Coordenadas inválidas.");
                return;
            }
        } else {
            loc = player.getLocation().getBlock().getLocation();
        }

        if (point == 1) {
            sel.setPos1(loc);
            player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Ponto 1 (pos1) definido em: "
                    + ChatColor.WHITE + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else {
            sel.setPos2(loc);
            player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Ponto 2 (pos2) definido em: "
                    + ChatColor.WHITE + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }

    private void handleExpand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("vert")) {
            player.sendMessage(ChatColor.RED + "Uso correto: /guard expand vert");
            return;
        }

        GuardSelection sel = guardManager.getOrCreateSelection(player.getUniqueId());
        if (!sel.expandVert()) {
            player.sendMessage(ChatColor.RED + "Você precisa marcar pos1 e pos2 antes de expandir!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Seleção expandida verticalmente do Y="
                + sel.getPos1().getBlockY() + " até Y=" + sel.getPos2().getBlockY() + "!");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso correto: /guard create <nome> [whitelist|blacklist]");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardSelection sel = guardManager.getOrCreateSelection(player.getUniqueId());

        if (!sel.isComplete()) {
            player.sendMessage(ChatColor.RED + "Você precisa definir pos1 e pos2 no mesmo mundo antes de criar!");
            player.sendMessage(ChatColor.GRAY + "Dica: Use /guard wand para selecionar os blocos.");
            return;
        }

        GuardType type = GuardType.BLACKLIST;
        if (args.length >= 3) {
            type = GuardType.fromString(args[2]);
            if (type == null) {
                player.sendMessage(ChatColor.RED + "Tipo inválido! Use 'whitelist' ou 'blacklist'.");
                return;
            }
        }

        if (!guardManager.createRegion(name, sel.getPos1(), sel.getPos2(), type)) {
            player.sendMessage(ChatColor.RED + "Já existe uma região com o nome '" + name + "'!");
            return;
        }

        GuardRegion region = guardManager.getRegion(name);
        player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Região '" + ChatColor.YELLOW + name
                + ChatColor.GREEN + "' criada com sucesso!");
        player.sendMessage(ChatColor.GRAY + "Tipo: " + ChatColor.WHITE + type.getFormattedName()
                + ChatColor.GRAY + " | Volume: " + ChatColor.WHITE + region.getVolume() + " blocos");
    }

    private void handleSetRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard setregion <nome> [pos1 pos2]");
            return;
        }

        // Se o comando for executado por jogador com pos1 pos2 já marcados
        if (sender instanceof Player player) {
            GuardSelection sel = guardManager.getOrCreateSelection(player.getUniqueId());
            if (sel.isComplete()) {
                String name = args[1].trim().toLowerCase(Locale.ROOT);
                if (guardManager.createRegion(name, sel.getPos1(), sel.getPos2(), GuardType.BLACKLIST)) {
                    sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Região '" + name + "' definida com sucesso a partir dos pontos selecionados!");
                    return;
                } else {
                    sender.sendMessage(ChatColor.RED + "A região '" + name + "' já existe!");
                    return;
                }
            }
        }

        sender.sendMessage(ChatColor.RED + "Selecione pos1 e pos2 com a Wand antes de definir a região!");
    }

    private void handleType(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard region <nome> <whitelist|blacklist>");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardRegion region = guardManager.getRegion(name);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
            return;
        }

        GuardType type = GuardType.fromString(args[2]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Tipo inválido! Use 'whitelist' ou 'blacklist'.");
            return;
        }

        region.setType(type);
        guardManager.saveData(true);
        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "O tipo da região '" + name
                + "' foi alterado para: " + type.getFormattedName());
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard delete <nome>");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        if (guardManager.deleteRegion(name)) {
            sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Região '" + name + "' removida com sucesso.");
        } else {
            sender.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
        }
    }

    private void handleFlag(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard flag <nome> <flag> <allow|deny>");
            sender.sendMessage(ChatColor.GRAY + "Flags: entry, exit, block-break, block-place, interact, pvp");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardRegion region = guardManager.getRegion(name);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
            return;
        }

        GuardFlag flag = GuardFlag.fromString(args[2]);
        if (flag == null) {
            sender.sendMessage(ChatColor.RED + "Flag desconhecida: " + args[2]);
            return;
        }

        String valStr = args[3].toLowerCase(Locale.ROOT);
        boolean allow = valStr.equals("allow") || valStr.equals("permitir") || valStr.equals("true") || valStr.equals("sim");

        region.setFlag(flag, allow);
        guardManager.saveData(true);

        String status = allow ? ChatColor.GREEN + "PERMITIDO" : ChatColor.RED + "NEGADO";
        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GRAY + "Flag " + ChatColor.YELLOW + flag.getDisplayName()
                + ChatColor.GRAY + " da região " + ChatColor.YELLOW + name + ChatColor.GRAY + " definida como " + status);
    }

    private void handleMessage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard message <nome> <mensagem|reset>");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardRegion region = guardManager.getRegion(name);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
            return;
        }

        if (args.length == 2 || args[2].equalsIgnoreCase("reset")) {
            region.setCustomMessage(null);
            guardManager.saveData(true);
            sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Mensagem personalizada resetada para a padrão.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) sb.append(" ");
            sb.append(args[i]);
        }
        String msg = sb.toString();
        region.setCustomMessage(msg);
        guardManager.saveData(true);

        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Mensagem da região atualizada para:");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    private void handleShow(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso correto: /guard show <nome> [segundos]");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardRegion region = guardManager.getRegion(name);
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
            return;
        }

        int seconds = 15;
        if (args.length >= 3) {
            try {
                seconds = Math.max(3, Math.min(60, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {}
        }

        guardManager.showRegion(player, region, seconds);
        player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Exibindo bordas da região '"
                + name + "' com partículas por " + seconds + " segundos.");
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso correto: /guard tp <nome>");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardRegion region = guardManager.getRegion(name);
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
            return;
        }

        Location center = region.getCenter();
        if (center == null || center.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "O mundo desta região não está carregado!");
            return;
        }

        player.teleport(center);
        player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Teleportado para o centro da região '" + name + "'!");
    }

    private void handleList(CommandSender sender) {
        Collection<GuardRegion> regions = guardManager.getAllRegions();
        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhuma região cadastrada no momento.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== [ Regiões Protegidas (" + regions.size() + ") ] ===");
        for (GuardRegion r : regions) {
            String color = r.getType() == GuardType.WHITELIST ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
            sender.sendMessage(ChatColor.GRAY + "• " + ChatColor.YELLOW + r.getName()
                    + ChatColor.GRAY + " (" + color + r.getType().getRawName() + ChatColor.GRAY + ") "
                    + ChatColor.DARK_GRAY + "- Mundo: " + ChatColor.WHITE + r.getWorldName()
                    + ChatColor.DARK_GRAY + " | Vol: " + ChatColor.WHITE + r.getVolume());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard info <nome>");
            return;
        }

        String name = args[1].trim().toLowerCase(Locale.ROOT);
        GuardRegion region = guardManager.getRegion(name);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Região '" + name + "' não encontrada.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== [ Detalhes da Região: " + region.getName() + " ] ===");
        sender.sendMessage(ChatColor.GRAY + "Tipo: " + ChatColor.WHITE + region.getType().getFormattedName());
        sender.sendMessage(ChatColor.GRAY + "Mundo: " + ChatColor.WHITE + region.getWorldName());
        sender.sendMessage(ChatColor.GRAY + "Coordenadas Min: " + ChatColor.WHITE + (int) region.getMinX() + ", " + (int) region.getMinY() + ", " + (int) region.getMinZ());
        sender.sendMessage(ChatColor.GRAY + "Coordenadas Max: " + ChatColor.WHITE + (int) region.getMaxX() + ", " + (int) region.getMaxY() + ", " + (int) region.getMaxZ());
        sender.sendMessage(ChatColor.GRAY + "Volume: " + ChatColor.WHITE + region.getVolume() + " blocos");

        if (region.getCustomMessage() != null) {
            sender.sendMessage(ChatColor.GRAY + "Mensagem: " + ChatColor.translateAlternateColorCodes('&', region.getCustomMessage()));
        }

        sender.sendMessage(ChatColor.GRAY + "Flags:");
        for (GuardFlag flag : GuardFlag.values()) {
            boolean allowed = region.isFlagAllowed(flag);
            String status = allowed ? ChatColor.GREEN + "ALLOW" : ChatColor.RED + "DENY";
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.YELLOW + flag.getKey() + ": " + status);
        }
    }

    private void handleReload(CommandSender sender) {
        guardManager.loadConfigOptions();
        guardManager.loadData();
        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Configurações e regiões recarregadas do disco com sucesso!");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== [ Ajuda do Módulo FurGuard ] ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " wand " + ChatColor.GRAY + "- Ferramenta de seleção de pontos");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " pos1 [x y z] " + ChatColor.GRAY + "- Define o ponto 1");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " pos2 [x y z] " + ChatColor.GRAY + "- Define o ponto 2");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " expand vert " + ChatColor.GRAY + "- Expande a seleção da camada -64 a 320");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " create <nome> [tipo] " + ChatColor.GRAY + "- Cria região (whitelist/blacklist)");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " region <nome> <tipo> " + ChatColor.GRAY + "- Altera o tipo da região");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " flag <nome> <flag> <allow|deny> " + ChatColor.GRAY + "- Altera flags de proteção");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " message <nome> <texto> " + ChatColor.GRAY + "- Configura mensagem ao colidir");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " show <nome> " + ChatColor.GRAY + "- Exibe as bordas com partículas");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " tp <nome> " + ChatColor.GRAY + "- Teleporta para o centro da região");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list " + ChatColor.GRAY + "- Lista as regiões ativas");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <nome> " + ChatColor.GRAY + "- Detalhes da região");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " delete <nome> " + ChatColor.GRAY + "- Remove a região");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload " + ChatColor.GRAY + "- Recarrega regiões do disco");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "wand", "pos1", "pos2", "expand", "create", "setregion", "region",
                    "type", "delete", "flag", "message", "show", "tp", "list", "info", "reload", "help"
            );
            return filterPrefix(subs, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            if (sub.equals("expand")) {
                return filterPrefix(Collections.singletonList("vert"), args[1]);
            }
            if (Arrays.asList("region", "type", "delete", "remove", "flag", "message", "msg", "show", "preview", "tp", "info").contains(sub)) {
                List<String> regionNames = guardManager.getAllRegions().stream()
                        .map(GuardRegion::getName)
                        .collect(Collectors.toList());
                return filterPrefix(regionNames, args[1]);
            }
        }

        if (args.length == 3) {
            if (sub.equals("create") || sub.equals("region") || sub.equals("type")) {
                return filterPrefix(Arrays.asList("blacklist", "whitelist"), args[2]);
            }
            if (sub.equals("flag")) {
                List<String> flags = Arrays.stream(GuardFlag.values())
                        .map(GuardFlag::getKey)
                        .collect(Collectors.toList());
                return filterPrefix(flags, args[2]);
            }
        }

        if (args.length == 4) {
            if (sub.equals("flag")) {
                return filterPrefix(Arrays.asList("allow", "deny"), args[3]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return list.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }
}
