package net.nextfur.fws.bukkit.lobby.events;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import net.nextfur.fws.bukkit.utils.PositionParser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class LobbyEvents implements Listener {

    private YamlDocument config;
    private FurWatchBukkit plugin;

    public LobbyEvents(FurWatchBukkit plugin, YamlDocument config) {
        this.config = config;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (config.getBoolean("Lobby.Spawn-On-Join", false) && !config.getString("Lobby.SpawnPos", null).isEmpty()) {
            Location spawnPos = PositionParser.deserialize(config.getString("Lobby.SpawnPos"));
            event.getPlayer().teleport(spawnPos);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!config.getBoolean("Lobby.Disable-Hunger", false)) return;
        event.setCancelled(true);
        event.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!config.getBoolean("Lobby.Disable-Damage", false)) return;

        var source = event.getDamageSource().getCausingEntity();
        if (source instanceof Player damager) {
            boolean isAdmin = damager.isOp() || damager.hasPermission("furwatch.admin");
            if (!isAdmin) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }
}
