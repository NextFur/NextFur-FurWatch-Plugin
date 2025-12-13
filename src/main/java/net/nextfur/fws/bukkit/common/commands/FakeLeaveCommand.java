package net.nextfur.fws.bukkit.common.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class FakeLeaveCommand {
    public static void execute(CommonCommands data, Player player, String command, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "✖ Uso incorreto!");
            player.sendMessage(ChatColor.GRAY + "  /fakeleave <username>");
            return;
        }

        String target = args[0];
        String joinMessage = ChatColor.YELLOW + target + " left the game";

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(joinMessage);
        }
    }
}
