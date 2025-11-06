package net.nextfur.fws.bukkit.generic.commands;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GenericCommands implements CommandExecutor {
    private FurWatchBukkit plugin;
    private YamlDocument config;

    public GenericCommands (FurWatchBukkit plugin, YamlDocument config) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        // meow meow
        return false;
    }
}
