package net.nextfur.fws.bukkit.generic.events;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GenericEvents implements Listener {
    private YamlDocument config;
    private FurWatchBukkit plugin;

    public GenericEvents(FurWatchBukkit plugin, YamlDocument config) {
        this.config = config;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // ;3
    }

}
