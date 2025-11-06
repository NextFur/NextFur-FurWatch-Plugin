package net.nextfur.fws.bukkit.common.events;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BannedItemsListener implements Listener {
    private FurWatchBukkit plugin;

    private String banMessage = "§cOpa! Um item banido foi removido do seu inventário. §5[ x%amount% %item% ]";

    public BannedItemsListener(FurWatchBukkit plugin) {
        this.plugin = plugin;
    }

    private boolean isBanned(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return plugin.getBannedItems().contains(item.getType().getKey().toString());
    }

    private void sendFeedbackMsg(ItemStack item, Player player) {
        String msg = plugin._getConfig().getString("Messages.BannedItem", banMessage);

        msg = msg.replaceAll("%amount%", String.valueOf(item.getAmount()));
        msg = msg.replaceAll("%item%", item.getType().getKey().toString());

        player.sendMessage(msg);
    }

    private void removeBannedItem(ItemStack item, Player player) {
        if (item == null) return;

        sendFeedbackMsg(item, player);

        item.setAmount(0);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();

        if (player.hasPermission("furwatch.admin")) return;

        for (ItemStack item : inv.getContents()) if (isBanned(item)) removeBannedItem(item, player);
        for (ItemStack item : inv.getArmorContents()) if (isBanned(item)) removeBannedItem(item, player);

        if (isBanned(player.getItemOnCursor())) player.setItemOnCursor(null);

        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("furwatch.admin")) return;

        if (isBanned(event.getCurrentItem())) {
            event.setCancelled(true);
            event.setCurrentItem(null);
            sendFeedbackMsg(event.getCurrentItem(), player);
        }

        if (isBanned(event.getCursor())) {
            event.setCancelled(true);
            event.setCursor(null);
            sendFeedbackMsg(event.getCursor(), player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (player.hasPermission("furwatch.admin")) return;

        if (isBanned(event.getItem().getItemStack())) {
            event.setCancelled(true);
            sendFeedbackMsg(event.getItem().getItemStack(), player);
            event.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("furwatch.admin")) return;

        if (isBanned(event.getItem())) {
            event.setCancelled(true);
            removeBannedItem(event.getItem(), player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("furwatch.admin")) {
            return;
        }

        if (isBanned(event.getItemInHand())) {
            event.setCancelled(true);
            sendFeedbackMsg(event.getItemInHand(), player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("furwatch.admin")) {
            return;
        }

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (isBanned(newItem)) {
            removeBannedItem(newItem, player);
        }
    }
}
