package net.nextfur.fws.bukkit.lobby.commands;

import net.nextfur.fws.bukkit.utils.PositionParser;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SetSpawnCommand {

    public static void execute(LobbyCommands data, Player player, String command, String[] args) {
        data.config.set("Lobby.SpawnPos", PositionParser.serialize(player.getLocation()));
        try {
            data.config.save();
            data.config.reload();
            player.sendMessage(ChatColor.GREEN + "[FurWatch] - Spawn lobby definido com sucesso!");
        } catch (Exception ignored) {}
    }
}
