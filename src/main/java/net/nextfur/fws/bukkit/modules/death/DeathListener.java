package net.nextfur.fws.bukkit.modules.death;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {
    private final FurWatchBukkit plugin;
    private final DeathManager deathManager;

    public DeathListener(FurWatchBukkit plugin, DeathManager deathManager) {
        this.plugin = plugin;
        this.deathManager = deathManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathManager.isEnabled()) {
            return;
        }

        Player player = event.getEntity();
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.trim().isEmpty()) {
            deathMessage = player.getName() + " morreu.";
        }

        Location loc = player.getLocation();

        PlayerDeathData data = deathManager.recordDeath(player, deathMessage, loc);

        if (deathManager.isNotifyOps()) {
            String rawTemplate = deathManager.getDeathAlertMessage();
            String formattedMessage = formatAlert(rawTemplate, player, deathMessage, loc, data.getDeathCount());

            String permission = deathManager.getPermission();
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.isOp() || (permission != null && !permission.isEmpty() && staff.hasPermission(permission))) {
                    staff.sendMessage(formattedMessage);
                }
            }

            if (deathManager.isLogToConsole()) {
                plugin._getLogger().info(formattedMessage);
            }
        }
    }

    private String formatAlert(String template, Player player, String deathMessage, Location loc, int totalDeaths) {
        String worldName = (loc != null && loc.getWorld() != null) ? loc.getWorld().getName() : "world";
        int x = loc != null ? loc.getBlockX() : 0;
        int y = loc != null ? loc.getBlockY() : 0;
        int z = loc != null ? loc.getBlockZ() : 0;

        String formatted = template
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%death_message%", deathMessage)
                .replace("%death_count%", String.valueOf(totalDeaths))
                .replace("%world%", worldName)
                .replace("%x%", String.valueOf(x))
                .replace("%y%", String.valueOf(y))
                .replace("%z%", String.valueOf(z));

        return DeathManager.colorize(formatted);
    }
}
