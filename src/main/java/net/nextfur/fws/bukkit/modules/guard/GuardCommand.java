package net.nextfur.fws.bukkit.modules.guard;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
            case "members":
            case "member":
                handleMembers(sender, args);
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

    private void handleMembers(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("help")) {
            sendMembersHelp(sender);
            return;
        }

        Set<String> actionKeywords = new HashSet<>(Arrays.asList("add", "remove", "list", "clear", "reset", "check", "has"));
        String regionName;
        String action;
        String targetArg = null;

        if (actionKeywords.contains(args[1].toLowerCase(Locale.ROOT))) {
            // Sintaxe alternativa/flexível: /guard members <action> <nome> [jogador]
            action = args[1].toLowerCase(Locale.ROOT);
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Uso correto: /guard members " + action + " <região>"
                        + (action.equals("list") || action.equals("clear") || action.equals("reset") ? "" : " <jogador|@a>"));
                return;
            }
            regionName = args[2].trim().toLowerCase(Locale.ROOT);
            if (args.length >= 4) {
                targetArg = args[3].trim();
            }
        } else {
            // Sintaxe padrão: /guard members <nome> <action> [jogador]
            regionName = args[1].trim().toLowerCase(Locale.ROOT);
            if (args.length == 2) {
                action = "list";
            } else {
                action = args[2].toLowerCase(Locale.ROOT);
                if (args.length >= 4) {
                    targetArg = args[3].trim();
                }
            }
        }

        GuardRegion region = guardManager.getRegion(regionName);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Região '" + regionName + "' não encontrada.");
            return;
        }

        switch (action) {
            case "list":
                handleMembersList(sender, region);
                break;
            case "add":
                handleMembersAdd(sender, region, targetArg);
                break;
            case "remove":
                handleMembersRemove(sender, region, targetArg);
                break;
            case "clear":
            case "reset":
                handleMembersClear(sender, region);
                break;
            case "check":
            case "has":
                handleMembersCheck(sender, region, targetArg);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Ação desconhecida '" + action + "'. Opções disponíveis: list, add, remove, clear, check.");
                break;
        }
    }

    private void handleMembersList(CommandSender sender, GuardRegion region) {
        Set<UUID> members = region.getMembers();
        if (members.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "A região '" + region.getName() + "' não possui nenhum membro cadastrado.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== [ Membros da Região: " + region.getName() + " (" + members.size() + ") ] ===");
        List<String> formatted = new ArrayList<>();
        for (UUID uuid : members) {
            String name = region.getMemberNames().get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                name = online.getName();
                formatted.add(ChatColor.GREEN + name + ChatColor.DARK_GRAY + " (Online)");
            } else {
                if (name == null && Bukkit.getServer() != null) {
                    //noinspection deprecation
                    OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                    if (off.getName() != null) name = off.getName();
                }
                if (name == null) name = uuid.toString().substring(0, 8);
                formatted.add(ChatColor.GRAY + name + ChatColor.DARK_GRAY + " (Offline)");
            }
        }

        if (formatted.size() <= 15) {
            for (String line : formatted) {
                sender.sendMessage(ChatColor.DARK_GRAY + " • " + line);
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + String.join(ChatColor.DARK_GRAY + ", " + ChatColor.GRAY, formatted));
        }
    }

    private void handleMembersAdd(CommandSender sender, GuardRegion region, String targetArg) {
        if (targetArg == null || targetArg.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard members " + region.getName() + " add <jogador|@a>");
            return;
        }

        if (targetArg.equalsIgnoreCase("@a")) {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Não há nenhum jogador online no servidor no momento.");
                return;
            }

            int addedCount = 0;
            for (Player p : onlinePlayers) {
                if (region.addMember(p.getUniqueId(), p.getName())) {
                    addedCount++;
                }
            }

            guardManager.saveData(true);
            sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Foram adicionados " + ChatColor.YELLOW + addedCount
                    + ChatColor.GREEN + " jogador(es) online à região '" + ChatColor.YELLOW + region.getName() + ChatColor.GREEN + "'!"
                    + ChatColor.GRAY + " (Total online: " + onlinePlayers.size() + ")");
            return;
        }

        Player online = Bukkit.getPlayerExact(targetArg);
        UUID uuid;
        String name;

        if (online != null) {
            uuid = online.getUniqueId();
            name = online.getName();
        } else {
            //noinspection deprecation
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetArg);
            uuid = off.getUniqueId();
            name = off.getName() != null ? off.getName() : targetArg;
        }

        if (region.isMember(uuid)) {
            sender.sendMessage(ChatColor.YELLOW + "O jogador '" + name + "' já possui acesso à região '" + region.getName() + "'.");
            return;
        }

        region.addMember(uuid, name);
        guardManager.saveData(true);
        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Jogador " + ChatColor.YELLOW + name
                + ChatColor.GREEN + " adicionado aos membros da região '" + ChatColor.YELLOW + region.getName() + ChatColor.GREEN + "' com sucesso!");
    }

    private void handleMembersRemove(CommandSender sender, GuardRegion region, String targetArg) {
        if (targetArg == null || targetArg.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard members " + region.getName() + " remove <jogador|@a>");
            return;
        }

        if (targetArg.equalsIgnoreCase("@a")) {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Não há nenhum jogador online no servidor no momento.");
                return;
            }

            int removedCount = 0;
            for (Player p : onlinePlayers) {
                if (region.removeMember(p.getUniqueId())) {
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                guardManager.saveData(true);
                sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Foram removidos " + ChatColor.YELLOW + removedCount
                        + ChatColor.GREEN + " jogador(es) online da região '" + ChatColor.YELLOW + region.getName() + ChatColor.GREEN + "'!");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Nenhum dos jogadores online atualmente era membro da região '" + region.getName() + "'.");
            }
            return;
        }

        // Tenta resolver por nome no cache de membros
        UUID targetUuid = null;
        String targetName = targetArg;

        for (Map.Entry<UUID, String> entry : region.getMemberNames().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(targetArg)) {
                targetUuid = entry.getKey();
                targetName = entry.getValue();
                break;
            }
        }

        if (targetUuid == null) {
            Player online = Bukkit.getPlayerExact(targetArg);
            if (online != null && region.isMember(online.getUniqueId())) {
                targetUuid = online.getUniqueId();
                targetName = online.getName();
            }
        }

        if (targetUuid == null) {
            //noinspection deprecation
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetArg);
            if (off != null && region.isMember(off.getUniqueId())) {
                targetUuid = off.getUniqueId();
                if (off.getName() != null) targetName = off.getName();
            }
        }

        if (targetUuid == null) {
            try {
                UUID parsed = UUID.fromString(targetArg);
                if (region.isMember(parsed)) {
                    targetUuid = parsed;
                }
            } catch (IllegalArgumentException ignored) {}
        }

        if (targetUuid == null || !region.isMember(targetUuid)) {
            sender.sendMessage(ChatColor.RED + "O jogador '" + targetArg + "' não é membro da região '" + region.getName() + "'.");
            return;
        }

        region.removeMember(targetUuid);
        guardManager.saveData(true);
        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Jogador " + ChatColor.YELLOW + targetName
                + ChatColor.GREEN + " removido dos membros da região '" + ChatColor.YELLOW + region.getName() + ChatColor.GREEN + "' com sucesso!");
    }

    private void handleMembersClear(CommandSender sender, GuardRegion region) {
        int total = region.getMembers().size();
        if (total == 0) {
            sender.sendMessage(ChatColor.YELLOW + "A região '" + region.getName() + "' não possui nenhum membro cadastrado.");
            return;
        }

        region.clearMembers();
        guardManager.saveData(true);
        sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Todos os " + ChatColor.YELLOW + total
                + ChatColor.GREEN + " membros da região '" + ChatColor.YELLOW + region.getName() + ChatColor.GREEN + "' foram removidos com sucesso!");
    }

    private void handleMembersCheck(CommandSender sender, GuardRegion region, String targetArg) {
        if (targetArg == null || targetArg.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /guard members " + region.getName() + " check <jogador>");
            return;
        }

        Player online = Bukkit.getPlayerExact(targetArg);
        boolean isMember = false;
        String targetName = targetArg;

        if (online != null) {
            isMember = region.isMember(online.getUniqueId()) || region.isMember(online.getName());
            targetName = online.getName();
        } else {
            for (Map.Entry<UUID, String> entry : region.getMemberNames().entrySet()) {
                if (entry.getValue().equalsIgnoreCase(targetArg)) {
                    isMember = true;
                    targetName = entry.getValue();
                    break;
                }
            }
            if (!isMember) {
                //noinspection deprecation
                OfflinePlayer off = Bukkit.getOfflinePlayer(targetArg);
                if (off != null && region.isMember(off.getUniqueId())) {
                    isMember = true;
                    if (off.getName() != null) targetName = off.getName();
                }
            }
        }

        if (isMember) {
            sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "O jogador " + ChatColor.YELLOW + targetName
                    + ChatColor.GREEN + " POSSUI acesso à região '" + ChatColor.YELLOW + region.getName() + ChatColor.GREEN + "'.");
        } else {
            sender.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.RED + "O jogador " + ChatColor.YELLOW + targetName
                    + ChatColor.RED + " NÃO possui acesso à região '" + ChatColor.YELLOW + region.getName() + ChatColor.RED + "'.");
        }
    }

    private void sendMembersHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== [ Comandos de Membros do FurGuard ] ===");
        sender.sendMessage(ChatColor.YELLOW + "/guard members <região> list " + ChatColor.GRAY + "- Lista membros com acesso à região");
        sender.sendMessage(ChatColor.YELLOW + "/guard members <região> add <jogador|@a> " + ChatColor.GRAY + "- Concede acesso (suporta @a para todos online)");
        sender.sendMessage(ChatColor.YELLOW + "/guard members <região> remove <jogador|@a> " + ChatColor.GRAY + "- Revoga acesso de um jogador ou todos online (@a)");
        sender.sendMessage(ChatColor.YELLOW + "/guard members <região> clear " + ChatColor.GRAY + "- Remove todos os membros da região");
        sender.sendMessage(ChatColor.YELLOW + "/guard members <região> check <jogador> " + ChatColor.GRAY + "- Verifica se um jogador tem acesso");
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

        Set<UUID> members = region.getMembers();
        sender.sendMessage(ChatColor.GRAY + "Membros com acesso (" + members.size() + "): "
                + (members.isEmpty() ? ChatColor.DARK_GRAY + "Nenhum" : ChatColor.WHITE + formatMembersSummary(region)));
    }

    private String formatMembersSummary(GuardRegion region) {
        Set<UUID> members = region.getMembers();
        if (members.isEmpty()) return "Nenhum";
        List<String> names = new ArrayList<>();
        for (UUID u : members) {
            String n = region.getMemberNames().get(u);
            if (n != null) {
                names.add(n);
            } else {
                Player p = Bukkit.getPlayer(u);
                if (p != null) names.add(p.getName());
                else names.add(u.toString().substring(0, 8));
            }
            if (names.size() >= 5) break;
        }
        String res = String.join(", ", names);
        if (members.size() > 5) {
            res += " (+" + (members.size() - 5) + " outros)";
        }
        return res;
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
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " members <nome> <add|remove|list> [jogador|@a] " + ChatColor.GRAY + "- Gerencia membros com acesso à região");
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
                    "type", "members", "member", "delete", "flag", "message", "show", "tp", "list", "info", "reload", "help"
            );
            return filterPrefix(subs, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("members") || sub.equals("member")) {
            List<String> actions = Arrays.asList("list", "add", "remove", "clear", "check");
            List<String> regionNames = guardManager.getAllRegions().stream()
                    .map(GuardRegion::getName)
                    .collect(Collectors.toList());

            if (args.length == 2) {
                List<String> suggestions = new ArrayList<>(regionNames);
                suggestions.addAll(actions);
                return filterPrefix(suggestions, args[1]);
            }

            if (args.length == 3) {
                if (actions.contains(args[1].toLowerCase(Locale.ROOT))) {
                    // /guard members <action> <região>
                    return filterPrefix(regionNames, args[2]);
                } else {
                    // /guard members <região> <action>
                    return filterPrefix(actions, args[2]);
                }
            }

            if (args.length == 4) {
                String act = actions.contains(args[1].toLowerCase(Locale.ROOT))
                        ? args[1].toLowerCase(Locale.ROOT)
                        : args[2].toLowerCase(Locale.ROOT);
                String regName = actions.contains(args[1].toLowerCase(Locale.ROOT))
                        ? args[2].toLowerCase(Locale.ROOT)
                        : args[1].toLowerCase(Locale.ROOT);

                GuardRegion reg = guardManager.getRegion(regName);

                if (act.equals("add")) {
                    List<String> players = new ArrayList<>();
                    players.add("@a");
                    players.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    return filterPrefix(players, args[3]);
                } else if (act.equals("remove")) {
                    List<String> removeTargets = new ArrayList<>();
                    removeTargets.add("@a");
                    if (reg != null) {
                        for (UUID u : reg.getMembers()) {
                            String name = reg.getMemberNames().get(u);
                            if (name != null) {
                                removeTargets.add(name);
                            } else {
                                removeTargets.add(u.toString());
                            }
                        }
                    }
                    if (removeTargets.size() <= 1) {
                        removeTargets.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    }
                    return filterPrefix(removeTargets, args[3]);
                } else if (act.equals("check")) {
                    List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    return filterPrefix(players, args[3]);
                }
            }

            return Collections.emptyList();
        }

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
