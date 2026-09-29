package net.nextfur.fws.bukkit.modules.guard;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GuardListener implements Listener {
    private final FurWatchBukkit plugin;
    private final GuardManager guardManager;

    public GuardListener(FurWatchBukkit plugin, GuardManager guardManager) {
        this.plugin = plugin;
        this.guardManager = guardManager;
    }

    /**
     * Otimização cirúrgica: descarta eventos quando não há transição de bloco
     * para preservar ciclos de CPU ao girar câmera ou micro-movimentos.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        // Filtro de coordenadas inteiras de bloco
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        GuardRegion violated = guardManager.checkMovementViolation(player, from, to);
        if (violated != null) {
            guardManager.applyRepulsion(player, from, to);
            GuardFlag flag = violated.getType() == GuardType.WHITELIST ? GuardFlag.EXIT : GuardFlag.ENTRY;
            guardManager.notifyViolation(player, violated, flag);
        }
    }

    /**
     * Impede que o jogador ultrapasse barreiras via Ender Pearl, Fruta do Coro ou teleportes.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        Player player = event.getPlayer();
        GuardRegion violated = guardManager.checkMovementViolation(player, from, to);
        if (violated != null) {
            event.setCancelled(true);
            GuardFlag flag = violated.getType() == GuardType.WHITELIST ? GuardFlag.EXIT : GuardFlag.ENTRY;
            guardManager.notifyViolation(player, violated, flag);
        }
    }

    /**
     * Impede que veículos (barcos, carrinhos e montarias) com jogadores ultrapassem barreiras.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleMove(VehicleMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (passenger instanceof Player player) {
                GuardRegion violated = guardManager.checkMovementViolation(player, from, to);
                if (violated != null) {
                    event.getVehicle().eject();
                    guardManager.applyRepulsion(player, from, to);
                    GuardFlag flag = violated.getType() == GuardType.WHITELIST ? GuardFlag.EXIT : GuardFlag.ENTRY;
                    guardManager.notifyViolation(player, violated, flag);
                    break;
                }
            }
        }
    }

    /**
     * Protege quebra de blocos em regiões com flag BLOCK_BREAK.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        GuardRegion violated = guardManager.checkFlagViolation(player, loc, GuardFlag.BLOCK_BREAK);
        if (violated != null) {
            event.setCancelled(true);
            guardManager.notifyViolation(player, violated, GuardFlag.BLOCK_BREAK);
        }
    }

    /**
     * Protege colocação de blocos em regiões com flag BLOCK_PLACE.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        GuardRegion violated = guardManager.checkFlagViolation(player, loc, GuardFlag.BLOCK_PLACE);
        if (violated != null) {
            event.setCancelled(true);
            guardManager.notifyViolation(player, violated, GuardFlag.BLOCK_PLACE);
        }
    }

    /**
     * Trata o uso da ferramenta Wand e previne interação com portas, baús, etc.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ignora mão secundária para evitar disparos duplicados
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();

        // Tratamento da FurGuard Wand
        if (event.getItem() != null && guardManager.isWandItem(event.getItem())) {
            if (player.isOp() || player.hasPermission("furwatch.admin") || player.hasPermission("furwatch.guard.command")) {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    event.setCancelled(true);
                    Location loc = event.getClickedBlock().getLocation();
                    GuardSelection sel = guardManager.getOrCreateSelection(player.getUniqueId());
                    sel.setPos1(loc);
                    player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Ponto 1 (pos1) definido em: "
                            + ChatColor.WHITE + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                    return;
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    event.setCancelled(true);
                    Location loc = event.getClickedBlock().getLocation();
                    GuardSelection sel = guardManager.getOrCreateSelection(player.getUniqueId());
                    sel.setPos2(loc);
                    player.sendMessage(ChatColor.GOLD + "[FurGuard] " + ChatColor.GREEN + "Ponto 2 (pos2) definido em: "
                            + ChatColor.WHITE + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                    return;
                }
            }
        }

        // Tratamento da flag INTERACT em blocos
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            GuardRegion violated = guardManager.checkFlagViolation(player, event.getClickedBlock().getLocation(), GuardFlag.INTERACT);
            if (violated != null) {
                event.setCancelled(true);
                guardManager.notifyViolation(player, violated, GuardFlag.INTERACT);
            }
        }
    }

    /**
     * Controla o combate entre jogadores em regiões com flag PVP desativada.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player damagerPlayer) {
            attacker = damagerPlayer;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        if (attacker == null) {
            return;
        }

        // Verifica a região da vítima e do agressor
        GuardRegion violated = guardManager.checkFlagViolation(attacker, victim.getLocation(), GuardFlag.PVP);
        if (violated == null) {
            violated = guardManager.checkFlagViolation(attacker, attacker.getLocation(), GuardFlag.PVP);
        }

        if (violated != null) {
            event.setCancelled(true);
            guardManager.notifyViolation(attacker, violated, GuardFlag.PVP);
        }
    }
}
