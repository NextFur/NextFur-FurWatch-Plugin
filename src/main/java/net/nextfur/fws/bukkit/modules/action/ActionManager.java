package net.nextfur.fws.bukkit.modules.action;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionManager {
    private final FurWatchBukkit plugin;
    private YamlDocument config;

    private boolean enabled;
    private double radius;
    private int durationSeconds;
    private int cooldownSeconds;
    private String customFormat;
    private String noOneNearbyMessage;
    private String usageMessage;
    private String cooldownMessage;
    private String disabledMessage;

    private final Map<String, ActionPreset> presets = new LinkedHashMap<>();
    private final Map<UUID, BukkitTask> activeActionBarTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public ActionManager(FurWatchBukkit plugin, YamlDocument config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        loadConfigOptions();
        loadPresets();

        if (this.enabled) {
            ActionCommand command = new ActionCommand(plugin, this);

            var cmdAcao = plugin.getCommand("acao");
            if (cmdAcao != null) {
                cmdAcao.setExecutor(command);
                cmdAcao.setTabCompleter(command);
            } else {
                plugin._getLogger().warn("Comando 'acao' não encontrado no plugin.yml!");
            }

            plugin._getLogger().info("Módulo de ações de roleplay (/acao, /action) carregado com sucesso!");
        } else {
            plugin._getLogger().info("Módulo de ações de roleplay está desativado na configuração.");
        }
    }

    public void loadConfigOptions() {
        this.enabled = config.getBoolean("RoleplayAction.Enabled", true);
        this.radius = config.getDouble("RoleplayAction.Radius", 15.0);
        this.durationSeconds = config.getInt("RoleplayAction.Duration-Seconds", 4);
        this.cooldownSeconds = config.getInt("RoleplayAction.Cooldown-Seconds", 2);
        this.customFormat = config.getString("RoleplayAction.Custom-Format", "&e* &6%player% &f%action% &e*");
        this.noOneNearbyMessage = config.getString("RoleplayAction.Messages.No-One-Nearby", "&7(Ninguém por perto viu sua ação)");
        this.usageMessage = config.getString("RoleplayAction.Messages.Usage", "&cUso: /acao <ação|preset> [alvo] &7- Digite &e/acao lista &7para ver presets.");
        this.cooldownMessage = config.getString("RoleplayAction.Messages.Cooldown", "&cAguarde %seconds%s para usar outra ação.");
        this.disabledMessage = config.getString("RoleplayAction.Messages.Disabled", "&cO sistema de ações de roleplay está desativado.");
    }

    public void loadPresets() {
        presets.clear();

        // 1. Built-in rich presets (minimum 15 presets)
        registerDefaultPreset(new ActionPreset("anger",
                "^(?:anger|raiva|irritad[oa]|brav[oa]|braveza)(?:\\s+(.+))?$",
                "Expressa raiva ou irritação",
                "&e* &6%player% &cfica com raiva! &e*",
                "&e* &6%player% &cfica com raiva de &f{target}&c! &e*"));

        registerDefaultPreset(new ActionPreset("happy",
                "^(?:happy|feliz|felicidade|alegre|alegria)(?:\\s+(.+))?$",
                "Expressa alegria ou felicidade",
                "&e* &6%player% &aestá muito feliz! &e*",
                "&e* &6%player% &afica feliz com &f{target}&a! &e*"));

        registerDefaultPreset(new ActionPreset("sad",
                "^(?:sad|triste|tristeza|deprimid[oa]|chorar|cry)(?:\\s+(.+))?$",
                "Expressa tristeza ou choro",
                "&e* &6%player% &9está triste e começa a chorar... &e*",
                "&e* &6%player% &9chora no ombro de &f{target}&9... &e*"));

        registerDefaultPreset(new ActionPreset("laugh",
                "^(?:laugh|rir|risad[aa]|gargalhad[aa]|haha+|lol)(?:\\s+(.+))?$",
                "Solta uma risada divertida",
                "&e* &6%player% &ecomeça a rir alegremente! &e*",
                "&e* &6%player% &eri junto com &f{target}&e! &e*"));

        registerDefaultPreset(new ActionPreset("hug",
                "^(?:hug|abraco|abraço|abracar|abraçar)(?:\\s+(.+))?$",
                "Dá ou pede um abraço",
                "&e* &6%player% &dquer um abraço quentinho. &e*",
                "&e* &6%player% &ddá um abraço carinhoso em &f{target}&d! &e*"));

        registerDefaultPreset(new ActionPreset("wave",
                "^(?:wave|acenar|aceno|tchau|ola|olá|oi)(?:\\s+(.+))?$",
                "Acena amigavelmente com a mão",
                "&e* &6%player% &eacena amigavelmente com a mão. &e*",
                "&e* &6%player% &eacena amigavelmente para &f{target}&e! &e*"));

        registerDefaultPreset(new ActionPreset("blush",
                "^(?:blush|corar|corad[oa]|vergonha)(?:\\s+(.+))?$",
                "Fica corado(a) de vergonha",
                "&e* &6%player% &dfica corado(a) e com vergonha... &e*",
                "&e* &6%player% &dfica corado(a) ao olhar para &f{target}&d... &e*"));

        registerDefaultPreset(new ActionPreset("scared",
                "^(?:scared|medo|assustad[oa]|temer)(?:\\s+(.+))?$",
                "Expressa medo ou susto",
                "&e* &6%player% &ctreme de medo e fica assustado(a)! &e*",
                "&e* &6%player% &cesconde-se atrás de &f{target}&c com medo! &e*"));

        registerDefaultPreset(new ActionPreset("tired",
                "^(?:tired|cansad[oa]|sono|bocejar|sleepy)(?:\\s+(.+))?$",
                "Expressa cansaço ou sono",
                "&e* &6%player% &7boceja com sono e aparenta cansaço. &e*",
                "&e* &6%player% &7apoia a cabeça em &f{target}&7 de tanto sono. &e*"));

        registerDefaultPreset(new ActionPreset("confused",
                "^(?:confused|confus[oa]|duvida|dúvida)(?:\\s+(.+))?$",
                "Fica confuso(a) com algo",
                "&e* &6%player% &eolha em volta confuso(a) e coça a cabeça. &e*",
                "&e* &6%player% &eolha para &f{target}&e sem entender nada. &e*"));

        registerDefaultPreset(new ActionPreset("dance",
                "^(?:dance|dancar|dançar|danca|dança)(?:\\s+(.+))?$",
                "Começa a dançar animadamente",
                "&e* &6%player% &bcomeça a dançar alegremente! &e*",
                "&e* &6%player% &bpuxa &f{target}&b para uma dança animada! &e*"));

        registerDefaultPreset(new ActionPreset("clap",
                "^(?:clap|aplaudir|palmas|aplauso)(?:\\s+(.+))?$",
                "Bate palmas entusiasticamente",
                "&e* &6%player% &ebate palmas calorosamente! &e*",
                "&e* &6%player% &eaplaude calorosamente a atitude de &f{target}&e! &e*"));

        registerDefaultPreset(new ActionPreset("sit",
                "^(?:sit|sentar|sentad[oa]|descansar)(?:\\s+(.+))?$",
                "Senta-se no chão para descansar",
                "&e* &6%player% &7senta-se no chão para descansar. &e*",
                "&e* &6%player% &7senta-se ao lado de &f{target}&7. &e*"));

        registerDefaultPreset(new ActionPreset("think",
                "^(?:think|pensar|pensativ[oa]|refletir)(?:\\s+(.+))?$",
                "Fica pensativo(a) refletindo",
                "&e* &6%player% &7fica pensativo(a), refletindo em silêncio... &e*",
                "&e* &6%player% &7pensa profundamente sobre o que &f{target}&7 disse. &e*"));

        registerDefaultPreset(new ActionPreset("wink",
                "^(?:wink|piscar|piscadela|piscada)(?:\\s+(.+))?$",
                "Dá uma piscadinha charmosa",
                "&e* &6%player% &6dá uma piscadinha charmosa. &e*",
                "&e* &6%player% &6dá uma piscadinha charmosa para &f{target}&6. &e*"));

        // 2. Load custom presets from config if present
        Section customSection = config.getSection("RoleplayAction.Custom-Presets");
        if (customSection != null) {
            for (Object keyObj : customSection.getKeys()) {
                String key = String.valueOf(keyObj);
                Section presetSec = customSection.getSection(key);
                if (presetSec != null) {
                    String regex = presetSec.getString("regex", "^(?:" + key + ")(?:\\s+(.+))?$");
                    String desc = presetSec.getString("description", "Ação personalizada " + key);
                    String defMsg = presetSec.getString("default", "&e* &6%player% &f" + key + " &e*");
                    String targetMsg = presetSec.getString("with-target", "&e* &6%player% &f" + key + " {target} &e*");
                    presets.put(key.toLowerCase(Locale.ROOT), new ActionPreset(key, regex, desc, defMsg, targetMsg));
                }
            }
        }
    }

    private void registerDefaultPreset(ActionPreset preset) {
        presets.put(preset.getId().toLowerCase(Locale.ROOT), preset);
    }

    public void reload(YamlDocument config) {
        this.config = config;
        loadConfigOptions();
        loadPresets();
    }

    public void broadcastAction(Player actor, String message) {
        final String formattedMessage = colorize(message);
        final double radiusSquared = radius * radius;
        final int duration = Math.max(1, durationSeconds);

        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player targetPlayer : actor.getWorld().getPlayers()) {
            if (targetPlayer.getLocation().distanceSquared(actor.getLocation()) <= radiusSquared) {
                nearbyPlayers.add(targetPlayer);
            }
        }

        if (nearbyPlayers.size() <= 1 && noOneNearbyMessage != null && !noOneNearbyMessage.isEmpty()) {
            actor.sendMessage(colorize(noOneNearbyMessage));
        }

        for (Player recipient : nearbyPlayers) {
            sendActionBarWithDuration(recipient, formattedMessage, duration);
        }
    }

    private void sendActionBarWithDuration(Player player, String message, int durationSeconds) {
        UUID uuid = player.getUniqueId();

        // Cancel previous task if any
        BukkitTask existing = activeActionBarTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        // Send immediately
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));

        if (durationSeconds <= 1) {
            return;
        }

        // Schedule repeating task every 20 ticks (1 second) to sustain the action bar display
        BukkitTask task = new BukkitRunnable() {
            int remaining = durationSeconds - 1;

            @Override
            public void run() {
                if (!player.isOnline() || remaining <= 0) {
                    activeActionBarTasks.remove(uuid);
                    cancel();
                    return;
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                remaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        activeActionBarTasks.put(uuid, task);
    }

    public boolean isOnCooldown(Player player) {
        if (player.isOp() || player.hasPermission("furwatch.admin") || player.hasPermission("furwatch.action.bypass")) {
            return false;
        }
        if (cooldownSeconds <= 0) {
            return false;
        }
        Long lastTime = cooldowns.get(player.getUniqueId());
        if (lastTime == null) {
            return false;
        }
        long diff = (System.currentTimeMillis() - lastTime) / 1000L;
        return diff < cooldownSeconds;
    }

    public long getRemainingCooldown(Player player) {
        Long lastTime = cooldowns.get(player.getUniqueId());
        if (lastTime == null) return 0;
        long diff = (System.currentTimeMillis() - lastTime) / 1000L;
        return Math.max(0, cooldownSeconds - diff);
    }

    public void applyCooldown(Player player) {
        if (cooldownSeconds > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public ActionPreset.MatchResult matchPreset(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        // Test presets
        for (ActionPreset preset : presets.values()) {
            ActionPreset.MatchResult result = preset.match(trimmed);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public String formatCustomAction(Player player, String actionText) {
        boolean canColor = player.isOp() || player.hasPermission("furwatch.admin") || player.hasPermission("furwatch.action.color");
        String cleanAction = canColor ? actionText : ChatColor.stripColor(colorize(actionText));

        return customFormat
                .replace("%player%", player.getName())
                .replace("{player}", player.getName())
                .replace("%action%", cleanAction)
                .replace("{action}", cleanAction);
    }

    public void shutdown() {
        for (BukkitTask task : activeActionBarTasks.values()) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
        activeActionBarTasks.clear();
        cooldowns.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getRadius() {
        return radius;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getUsageMessage() {
        return usageMessage;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }

    public String getDisabledMessage() {
        return disabledMessage;
    }

    public Collection<ActionPreset> getPresets() {
        return Collections.unmodifiableCollection(presets.values());
    }

    public ActionPreset getPreset(String id) {
        return presets.get(id.toLowerCase(Locale.ROOT));
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
