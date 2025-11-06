package net.nextfur.fws.bukkit.lobby.commands;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LobbyCommands implements CommandExecutor {
    public FurWatchBukkit plugin;
    public YamlDocument config;

    public LobbyCommands (FurWatchBukkit plugin, YamlDocument config) {
        this.config = config;
        this.plugin = plugin;

        plugin.getCommand("setspawn").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "Apenas jogadores podem executar este comando!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setspawn") && commandSender.hasPermission(command.getPermission())) {
            SetSpawnCommand.execute(this, (Player) commandSender, s, strings);
        }

        return true;
    }
}
