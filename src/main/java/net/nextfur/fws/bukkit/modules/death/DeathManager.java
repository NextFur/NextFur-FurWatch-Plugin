package net.nextfur.fws.bukkit.modules.death;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DeathManager {
    private final FurWatchBukkit plugin;
    private YamlDocument config;
    private final Gson gson;
    private final File dataFile;

    private final Map<UUID, PlayerDeathData> deathDataMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuidMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicBoolean pendingSave = new AtomicBoolean(false);

    private boolean enabled;
    private boolean notifyOps;
    private String permission;
    private boolean logToConsole;
    private String deathAlertMessage;

    public DeathManager(FurWatchBukkit plugin, YamlDocument config) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "deaths.json");
    }

    public void initialize() {
        loadConfigOptions();
        loadData();

        if (this.enabled) {
            DeathListener listener = new DeathListener(plugin, this);
            DeathCommand command = new DeathCommand(plugin, this);

            Bukkit.getPluginManager().registerEvents(listener, plugin);

            var cmd = plugin.getCommand("deathcount");
            if (cmd != null) {
                cmd.setExecutor(command);
                cmd.setTabCompleter(command);
            } else {
                plugin._getLogger().warn("Comando 'deathcount' não encontrado no plugin.yml!");
            }

            plugin._getLogger().info("Módulo de rastreamento de mortes carregado com sucesso!");
        } else {
            plugin._getLogger().info("Módulo de rastreamento de mortes está desativado na configuração.");
        }
    }

    public void loadConfigOptions() {
        this.enabled = config.getBoolean("DeathTracker.Enabled", true);
        this.notifyOps = config.getBoolean("DeathTracker.Notify-Ops", true);
        this.permission = config.getString("DeathTracker.Permission", "furwatch.admin");
        this.logToConsole = config.getBoolean("DeathTracker.Log-To-Console", true);
        this.deathAlertMessage = config.getString("DeathTracker.Messages.Death-Alert",
                "&8[&6FurWatch&8] &c%player% &7morreu! Causa: &f%death_message% &7| Mortes totais: &e%death_count% &7| Local: &f%world% &8(&f%x%&7, &f%y%&7, &f%z%&8)");
    }

    public void reload(YamlDocument config) {
        this.config = config;
        loadConfigOptions();
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, PlayerDeathData>>() {}.getType();
            Map<String, PlayerDeathData> loaded = gson.fromJson(reader, type);

            if (loaded != null) {
                deathDataMap.clear();
                nameToUuidMap.clear();

                for (Map.Entry<String, PlayerDeathData> entry : loaded.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        PlayerDeathData data = entry.getValue();
                        deathDataMap.put(uuid, data);
                        if (data.getLastKnownName() != null) {
                            nameToUuidMap.put(data.getLastKnownName().toLowerCase(Locale.ROOT), uuid);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            plugin._getLogger().error("Erro ao carregar banco de dados de mortes (deaths.json): " + e.getMessage());
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
        File tempFile = new File(dataFile.getParentFile(), "deaths.json.tmp");
        try {
            Map<String, PlayerDeathData> serializableMap = new HashMap<>();
            for (Map.Entry<UUID, PlayerDeathData> entry : deathDataMap.entrySet()) {
                serializableMap.put(entry.getKey().toString(), entry.getValue());
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                gson.toJson(serializableMap, writer);
            }

            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            plugin._getLogger().error("Erro ao salvar arquivo deaths.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerDeathData getOrCreateData(UUID uuid, String playerName) {
        return deathDataMap.compute(uuid, (id, existing) -> {
            if (existing == null) {
                existing = new PlayerDeathData(uuid, playerName);
            } else if (playerName != null && !playerName.isEmpty()) {
                existing.setLastKnownName(playerName);
            }
            if (playerName != null) {
                nameToUuidMap.put(playerName.toLowerCase(Locale.ROOT), uuid);
            }
            return existing;
        });
    }

    public PlayerDeathData getData(UUID uuid) {
        return deathDataMap.get(uuid);
    }

    public PlayerDeathData getData(String playerName) {
        if (playerName == null) return null;
        UUID uuid = nameToUuidMap.get(playerName.toLowerCase(Locale.ROOT));
        if (uuid != null) {
            return deathDataMap.get(uuid);
        }

        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return getOrCreateData(online.getUniqueId(), online.getName());
        }

        return null;
    }

    public PlayerDeathData recordDeath(Player player, String deathMessage, Location location) {
        PlayerDeathData data = getOrCreateData(player.getUniqueId(), player.getName());
        data.incrementDeathCount();
        data.setLastDeathTimestamp(System.currentTimeMillis());
        data.setLastDeathMessage(deathMessage != null ? deathMessage : "Morte desconhecida");

        if (location != null && location.getWorld() != null) {
            data.setLastLocation(String.format("%s, %d, %d, %d",
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()));
        }

        saveData(true);
        return data;
    }

    public boolean setDeathCount(UUID uuid, String playerName, int count) {
        PlayerDeathData data = getOrCreateData(uuid, playerName);
        data.setDeathCount(count);
        saveData(true);
        return true;
    }

    public boolean addDeathCount(UUID uuid, String playerName, int amount) {
        PlayerDeathData data = getOrCreateData(uuid, playerName);
        data.setDeathCount(data.getDeathCount() + amount);
        saveData(true);
        return true;
    }

    public boolean resetDeathCount(UUID uuid, String playerName) {
        PlayerDeathData data = getOrCreateData(uuid, playerName);
        data.setDeathCount(0);
        saveData(true);
        return true;
    }

    public List<PlayerDeathData> getTopDeaths(int limit) {
        return deathDataMap.values().stream()
                .filter(d -> d.getDeathCount() > 0)
                .sorted((a, b) -> Integer.compare(b.getDeathCount(), a.getDeathCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void shutdown() {
        plugin._getLogger().info("Salvando dados de mortes...");
        saveData(false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNotifyOps() {
        return notifyOps;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public String getDeathAlertMessage() {
        return deathAlertMessage;
    }

    public FurWatchBukkit getPlugin() {
        return plugin;
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
