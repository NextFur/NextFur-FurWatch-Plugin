package net.nextfur.fws.bukkit.common.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SudoCommand {
    public static void execute(CommonCommands data, Player player, String command, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "✖ Uso incorreto!");
            player.sendMessage(ChatColor.GRAY + "  /sudo <jogador> <comando>");
            player.sendMessage(ChatColor.DARK_GRAY + "  Exemplo: " + ChatColor.YELLOW + "/sudo FNPC13 g oi");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "✖ Jogador " + ChatColor.YELLOW + args[0] + ChatColor.RED + " não encontrado ou offline.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String cmd = sb.toString().trim();

        boolean success = Bukkit.dispatchCommand(target, cmd);

        if (success) {
            player.sendMessage(ChatColor.GREEN + "✔ Sucesso! " + ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " executou: " + ChatColor.WHITE + "/" + cmd);
        } else {
            player.sendMessage(ChatColor.RED + "✖ Ocorreu um erro ao tentar executar o comando como " + ChatColor.YELLOW + target.getName() + ChatColor.RED + ".");
        }
    }
}
