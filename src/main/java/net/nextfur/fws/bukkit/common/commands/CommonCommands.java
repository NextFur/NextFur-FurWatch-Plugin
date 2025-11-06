package net.nextfur.fws.bukkit.common.commands;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommonCommands implements CommandExecutor {
    private final FurWatchBukkit plugin;

    public CommonCommands (FurWatchBukkit plugin) {
        this.plugin = plugin;

        plugin.getCommand("banitem").setExecutor(this);
        plugin.getCommand("furwatch").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "Apenas jogadores podem executar este comando!");
            return true;
        }

        if (s.equalsIgnoreCase("banitem") && commandSender.hasPermission(command.getPermission())) {
            BanItemCommand.execute(this, (Player) commandSender, s, strings);
        }

        if (s.equalsIgnoreCase("furwatch") && commandSender.hasPermission(command.getPermission())) {
            FurWatchCommand.execute(this, (Player) commandSender, s, strings);
        }

        return true;
    }

    public FurWatchBukkit getPlugin() {
        return this.plugin;
    }
}
