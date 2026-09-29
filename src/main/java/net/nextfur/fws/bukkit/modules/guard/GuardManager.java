package net.nextfur.fws.bukkit.modules.guard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuardManager {
    private final FurWatchBukkit plugin;
    private final YamlDocument config;
    private final Gson gson;
    private final File dataFile;
    private final NamespacedKey wandKey;

    private boolean enabled;
    private double repulsionForce;
    private String notificationType;
    private boolean particleEffects;
    private boolean soundEffects;

    // Mensagens padrão
    private String blacklistEntryMsg;
    private String whitelistExitMsg;
    private String blockBreakMsg;
    private String blockPlaceMsg;
    private String interactMsg;
    private String pvpMsg;

    // Estruturas de dados em memória
    private final Map<String, GuardRegion> regionsByName = new ConcurrentHashMap<>();
    private final Map<String, List<GuardRegion>> regionsByWorld = new ConcurrentHashMap<>();
    private final Map<UUID, GuardSelection> playerSelections = new ConcurrentHashMap<>();
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> activeVisualizers = new ConcurrentHashMap<>();

    // Controle de salvamento atômico assíncrono
    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicBoolean pendingSave = new AtomicBoolean(false);

    public GuardManager(FurWatchBukkit plugin, YamlDocument config) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.wandKey = new NamespacedKey(plugin, "guard_wand");

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "guards.json");
    }

    public void initialize() {
        loadConfigOptions();
        loadData();

        if (this.enabled) {
            plugin._getLogger().info("Módulo Guard (/guard) carregado com " + regionsByName.size() + " regiões ativas.");
        } else {
            plugin._getLogger().info("Módulo Guard está desativado na configuração.");
        }
    }

    public void loadConfigOptions() {
        this.enabled = config.getBoolean("Guard.Enabled", true);
        this.repulsionForce = config.getDouble("Guard.Repulsion-Force", 0.35);
        this.notificationType = config.getString("Guard.Notification-Type", "ACTION_BAR");
        this.particleEffects = config.getBoolean("Guard.Particle-Effects", true);
        this.soundEffects = config.getBoolean("Guard.Sound-Effects", true);

        this.blacklistEntryMsg = config.getString("Guard.Messages.Blacklist-Entry",
                "&8[&6FurWatch&8] &cVocê não pode entrar nesta região restrita!");
        this.whitelistExitMsg = config.getString("Guard.Messages.Whitelist-Exit",
                "&8[&6FurWatch&8] &cVocê não pode sair desta área protegida!");
        this.blockBreakMsg = config.getString("Guard.Messages.Block-Break",
                "&8[&6FurWatch&8] &cVocê não tem permissão para quebrar blocos aqui!");
        this.blockPlaceMsg = config.getString("Guard.Messages.Block-Place",
                "&8[&6FurWatch&8] &cVocê não tem permissão para colocar blocos aqui!");
        this.interactMsg = config.getString("Guard.Messages.Interact",
                "&8[&6FurWatch&8] &cVocê não tem permissão para interagir aqui!");
        this.pvpMsg = config.getString("Guard.Messages.PvP",
                "&8[&6FurWatch&8] &cO combate entre jogadores está desativado nesta região!");
    }

    // Persistência de Dados (JSON Assíncrono com escrita atômica)

    public void loadData() {
        regionsByName.clear();
        regionsByWorld.clear();

        if (!dataFile.exists()) {
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, GuardRegion>>() {}.getType();
            Map<String, GuardRegion> loaded = gson.fromJson(reader, type);

            if (loaded != null) {
                for (GuardRegion region : loaded.values()) {
                    if (region.getName() != null && region.getWorldName() != null) {
                        region.updateBoundingBox();
                        regionsByName.put(region.getName().toLowerCase(Locale.ROOT), region);
                    }
                }
            }
            rebuildWorldIndex();
        } catch (Exception e) {
            plugin._getLogger().error("Erro ao carregar data/guards.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveData(boolean async) {
        if (async && Bukkit.isPrimaryThread()) {
            if (isSaving.compareAndSet(false, true)) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        performSave();
                    } finally {
                        isSaving.set(false);
                        if (pendingSave.getAndSet(false)) {
                            saveData(true);
                        }
                    }
                });
            } else {
                pendingSave.set(true);
            }
        } else {
            performSave();
        }
    }

    private synchronized void performSave() {
        File tempFile = new File(dataFile.getParentFile(), "guards.json.tmp");
        try {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                gson.toJson(regionsByName, writer);
            }
            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            plugin._getLogger().error("Erro ao salvar arquivo guards.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void rebuildWorldIndex() {
        regionsByWorld.clear();
        for (GuardRegion region : regionsByName.values()) {
            String world = region.getWorldName().toLowerCase(Locale.ROOT);
            regionsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(region);
        }

        // Ordena por prioridade decrescente (maior prioridade avaliada primeiro)
        for (List<GuardRegion> list : regionsByWorld.values()) {
            list.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        }
    }

    // Gerenciamento de Regiões

    public boolean createRegion(String name, Location p1, Location p2, GuardType type) {
        if (name == null || name.trim().isEmpty()) return false;
        String key = name.toLowerCase(Locale.ROOT).trim();
        if (regionsByName.containsKey(key)) {
            return false;
        }

        GuardRegion region = GuardRegion.fromLocations(key, p1, p2, type);
        regionsByName.put(key, region);
        rebuildWorldIndex();
        saveData(true);
        return true;
    }

    public boolean deleteRegion(String name) {
        if (name == null) return false;
        String key = name.toLowerCase(Locale.ROOT).trim();
        GuardRegion removed = regionsByName.remove(key);
        if (removed != null) {
            cancelVisualizer(key);
            rebuildWorldIndex();
            saveData(true);
            return true;
        }
        return false;
    }

    public GuardRegion getRegion(String name) {
        if (name == null) return null;
        return regionsByName.get(name.toLowerCase(Locale.ROOT).trim());
    }

    public Collection<GuardRegion> getAllRegions() {
        return Collections.unmodifiableCollection(regionsByName.values());
    }

    public List<GuardRegion> getRegionsInWorld(String worldName) {
        if (worldName == null) return Collections.emptyList();
        List<GuardRegion> list = regionsByWorld.get(worldName.toLowerCase(Locale.ROOT));
        return list != null ? list : Collections.emptyList();
    }

    // Ferramenta Wand e Seleção

    public ItemStack createWandItem() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "FurGuard Wand");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Ferramenta de seleção de regiões");
            lore.add(ChatColor.YELLOW + "Botão Esquerdo: " + ChatColor.WHITE + "Define Ponto 1 (pos1)");
            lore.add(ChatColor.YELLOW + "Botão Direito: " + ChatColor.WHITE + "Define Ponto 2 (pos2)");
            lore.add(ChatColor.DARK_GRAY + "Use /guard create <nome> [tipo]");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isWandItem(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public GuardSelection getOrCreateSelection(UUID playerId) {
        return playerSelections.computeIfAbsent(playerId, k -> new GuardSelection());
    }

    public void clearSelection(UUID playerId) {
        playerSelections.remove(playerId);
    }

    // Verificação de Permissões e Bypass

    public boolean hasBypass(Player player, GuardRegion region) {
        if (player == null) return true;
        if (player.isOp()) return true;
        if (player.hasPermission("furwatch.admin")) return true;
        if (player.hasPermission("furwatch.guard.bypass")) return true;

        if (region != null) {
            if (region.getBypassPermission() != null && !region.getBypassPermission().isEmpty()) {
                if (player.hasPermission(region.getBypassPermission())) {
                    return true;
                }
            }
            if (player.hasPermission("furwatch.guard.bypass." + region.getName())) {
                return true;
            }
        }
        return false;
    }

    // Checagem de Movimento e Violações

    /**
     * Verifica se o movimento entre 'from' e 'to' viola alguma barreira.
     * Retorna a região violada ou null caso permitido.
     */
    public GuardRegion checkMovementViolation(Player player, Location from, Location to) {
        if (!enabled || player == null || from == null || to == null || to.getWorld() == null) {
            return null;
        }

        if (hasBypass(player, null)) {
            return null;
        }

        List<GuardRegion> worldRegions = getRegionsInWorld(to.getWorld().getName());
        if (worldRegions.isEmpty()) {
            return null;
        }

        for (GuardRegion region : worldRegions) {
            if (hasBypass(player, region)) {
                continue;
            }

            boolean wasIn = region.contains(from);
            boolean isIn = region.contains(to);

            if (region.getType() == GuardType.BLACKLIST) {
                // Tentando entrar em uma Blacklist
                if (!wasIn && isIn && !region.isFlagAllowed(GuardFlag.ENTRY)) {
                    return region;
                }
            } else if (region.getType() == GuardType.WHITELIST) {
                // Tentando sair de uma Whitelist
                if (wasIn && !isIn && !region.isFlagAllowed(GuardFlag.EXIT)) {
                    return region;
                }
            }
        }

        return null;
    }

    /**
     * Verifica se uma ação em uma coordenada viola uma flag específica.
     */
    public GuardRegion checkFlagViolation(Player player, Location location, GuardFlag flag) {
        if (!enabled || location == null || location.getWorld() == null) {
            return null;
        }

        if (player != null && hasBypass(player, null)) {
            return null;
        }

        List<GuardRegion> worldRegions = getRegionsInWorld(location.getWorld().getName());
        for (GuardRegion region : worldRegions) {
            if (region.contains(location)) {
                if (player != null && hasBypass(player, region)) {
                    continue;
                }
                if (!region.isFlagAllowed(flag)) {
                    return region;
                }
            }
        }

        return null;
    }

    // Feedback, Notificações e Efeitos

    public void notifyViolation(Player player, GuardRegion region, GuardFlag flag) {
        if (player == null) return;

        // Anti-spam de mensagens (cooldown de 2 segundos)
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(player.getUniqueId());
        if (last != null && (now - last) < 2000L) {
            return;
        }
        messageCooldowns.put(player.getUniqueId(), now);

        String message = null;
        if (region != null && region.getCustomMessage() != null && !region.getCustomMessage().isEmpty()) {
            message = region.getCustomMessage();
        } else {
            message = switch (flag) {
                case ENTRY -> blacklistEntryMsg;
                case EXIT -> whitelistExitMsg;
                case BLOCK_BREAK -> blockBreakMsg;
                case BLOCK_PLACE -> blockPlaceMsg;
                case INTERACT -> interactMsg;
                case PVP -> pvpMsg;
            };
        }

        String formatted = ChatColor.translateAlternateColorCodes('&', message);

        if ("CHAT".equalsIgnoreCase(notificationType)) {
            player.sendMessage(formatted);
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatted));
        }

        if (soundEffects) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
        }

        if (particleEffects) {
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.2, 0), 5, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /**
     * Aplica vetor de repulsão no jogador para dar feedback tátil e evitar rubberband.
     */
    public void applyRepulsion(Player player, Location from, Location to) {
        if (player == null || from == null || to == null) return;

        Vector direction = from.toVector().subtract(to.toVector());
        if (direction.lengthSquared() < 0.0001) {
            direction = player.getLocation().getDirection().multiply(-1.0);
        }

        direction.normalize().multiply(repulsionForce);
        direction.setY(0.12); // Leve impulso para cima para descolar do chão suavemente

        player.teleport(from);
        player.setVelocity(direction);
    }

    // Visualização da BoundingBox com Partículas (/guard show <nome>)

    public boolean showRegion(Player player, GuardRegion region, int durationSeconds) {
        if (player == null || region == null) return false;
        String key = region.getName();

        cancelVisualizer(key);

        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) return false;

        Particle.DustOptions dustOptions = region.getType() == GuardType.WHITELIST
                ? new Particle.DustOptions(Color.fromRGB(50, 220, 50), 1.2f)
                : new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.2f);

        double minX = region.getMinX();
        double minY = region.getMinY();
        double minZ = region.getMinZ();
        double maxX = region.getMaxX();
        double maxY = region.getMaxY();
        double maxZ = region.getMaxZ();

        BukkitTask task = new BukkitRunnable() {
            int ticksLeft = durationSeconds * 2; // Roda a cada 10 ticks (0.5s)

            @Override
            public void run() {
                if (!player.isOnline() || ticksLeft-- <= 0) {
                    cancel();
                    activeVisualizers.remove(key);
                    return;
                }

                // Desenha as 12 arestas do paralelepípedo
                drawBoxEdges(player, world, minX, minY, minZ, maxX, maxY, maxZ, dustOptions);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        activeVisualizers.put(key, task);
        return true;
    }

    public void cancelVisualizer(String regionName) {
        if (regionName == null) return;
        BukkitTask task = activeVisualizers.remove(regionName.toLowerCase(Locale.ROOT));
        if (task != null) {
            task.cancel();
        }
    }

    private void drawBoxEdges(Player viewer, World world, double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ, Particle.DustOptions dust) {
        double step = 1.0;

        // 4 arestas no eixo X
        for (double x = minX; x <= maxX; x += step) {
            spawnEdgeParticle(viewer, world, x, minY, minZ, dust);
            spawnEdgeParticle(viewer, world, x, maxY, minZ, dust);
            spawnEdgeParticle(viewer, world, x, minY, maxZ, dust);
            spawnEdgeParticle(viewer, world, x, maxY, maxZ, dust);
        }

        // 4 arestas no eixo Y
        for (double y = minY; y <= maxY; y += step) {
            spawnEdgeParticle(viewer, world, minX, y, minZ, dust);
            spawnEdgeParticle(viewer, world, maxX, y, minZ, dust);
            spawnEdgeParticle(viewer, world, minX, y, maxZ, dust);
            spawnEdgeParticle(viewer, world, maxX, y, maxZ, dust);
        }

        // 4 arestas no eixo Z
        for (double z = minZ; z <= maxZ; z += step) {
            spawnEdgeParticle(viewer, world, minX, minY, z, dust);
            spawnEdgeParticle(viewer, world, maxX, minY, z, dust);
            spawnEdgeParticle(viewer, world, minX, maxY, z, dust);
            spawnEdgeParticle(viewer, world, maxX, maxY, z, dust);
        }
    }

    private void spawnEdgeParticle(Player viewer, World world, double x, double y, double z, Particle.DustOptions dust) {
        Location loc = new Location(world, x, y, z);
        if (loc.distanceSquared(viewer.getLocation()) <= 4096.0) { // Raio de até 64 blocos
            viewer.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }

    public void shutdown() {
        for (BukkitTask task : activeVisualizers.values()) {
            task.cancel();
        }
        activeVisualizers.clear();

        plugin._getLogger().info("Salvando regiões do Guard...");
        saveData(false);
    }

    // Getters

    public FurWatchBukkit getPlugin() {
        return plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
