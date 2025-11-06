package net.nextfur.fws.bukkit.common.events;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Bukkit;

public class CommonEvents {
    private final FurWatchBukkit plugin;

    public CommonEvents (FurWatchBukkit plugin) {
        this.plugin = plugin;

        Bukkit.getServer().getPluginManager().registerEvents(new BannedItemsListener(plugin), plugin);
    }
}
