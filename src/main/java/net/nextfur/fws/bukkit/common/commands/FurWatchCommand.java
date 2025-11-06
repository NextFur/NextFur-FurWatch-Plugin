package net.nextfur.fws.bukkit.common.commands;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class FurWatchCommand {
    public static void execute(CommonCommands data, Player player, String command, String[] args) {
        FurWatchBukkit plugin = data.getPlugin();

        if (args.length < 2 || !args[0].equalsIgnoreCase("setserver")) {
            player.sendMessage(ChatColor.YELLOW + "Uso correto: /furwatch setserver <lobby|generic>");
            return;
        }

        String role = args[1].toLowerCase();
        if (!role.equals("lobby") && !role.equals("generic")) {
            player.sendMessage(ChatColor.RED + "Cargo inválido. Use 'lobby' ou 'generic'.");
            return;
        }

        try {
            player.sendMessage(ChatColor.GOLD + "Configurando o servidor como '" + role + "'...");
            plugin.initialize(role);
            player.sendMessage(ChatColor.GREEN + "Plugin configurado e carregado com sucesso!");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Ocorreu um erro ao salvar a configuração. Verifique o console.");
            plugin._getLogger().error("Falha ao realizar setup inicial: " + e.getMessage());
            e.printStackTrace();
        }


    }
}
