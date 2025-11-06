package net.nextfur.fws.bukkit;

import com.google.gson.Gson;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.nextfur.fws.api.APIResponse;
import net.nextfur.fws.api.NextFurAPI;
import net.nextfur.fws.bukkit.common.commands.CommonCommands;
import net.nextfur.fws.bukkit.common.events.CommonEvents;
import net.nextfur.fws.bukkit.generic.commands.GenericCommands;
import net.nextfur.fws.bukkit.generic.events.GenericEvents;
import net.nextfur.fws.bukkit.lobby.commands.LobbyCommands;
import net.nextfur.fws.bukkit.lobby.events.LobbyEvents;
import net.nextfur.fws.bukkit.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FurWatchBukkit extends JavaPlugin {
    private FurWatchBukkit plugin;
    private final Logger LOGGER = new Logger(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "FurWatch" + ChatColor.DARK_GRAY + "] ");
    private YamlDocument config;
    private boolean isConfigured = false;
    private Gson gson;

    private final List<String> bannedItems = new ArrayList<>();
    private NextFurAPI api;

    @Override
    public void onEnable() {
        this.plugin = this;
        gson = new Gson();

        new CommonCommands(this);
        new CommonEvents(this);

        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            LOGGER.warn("----------------------------------------------------");
            LOGGER.warn(" config.yml não encontrado! O plugin está em modo de setup.");
            LOGGER.warn(" ");
            LOGGER.warn(" Por favor, entre no servidor e digite:");
            LOGGER.warn(" /furwatch setserver <lobby|generic>");
            LOGGER.warn("----------------------------------------------------");
            return;
        }

        try {
            this.config = YamlDocument.create(configFile);

            String role = config.getString("Server-Role");
            if (role == null || role.isEmpty()) {
                LOGGER.error("----------------------------------------------------");
                LOGGER.error(" ERRO: 'Server-Role' não encontrado no config.yml!");
                LOGGER.error(" Delete o config.yml e reinicie para reconfigurar.");
                LOGGER.error("----------------------------------------------------");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            initialize(role);
            this.isConfigured = true;

        } catch (Exception e) {
            LOGGER.error("Falha ao carregar config.yml! Desligando...");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void initialize(String role) {
        try {
            if (!this.isConfigured) {
                setupInitialConfig(role);
            }

            this.api = new NextFurAPI(
                    config.getString("FurGuard.ApiUrl"),
                    config.getString("FurGuard.ApiKey")
            );

            syncBannedItems();

            if (role.equalsIgnoreCase("lobby")) {
                new LobbyCommands(this, this.config);
                getServer().getPluginManager().registerEvents(new LobbyEvents(this, config), this);
            } else {
                new GenericCommands(this, this.config);
                getServer().getPluginManager().registerEvents(new GenericEvents(this, config), this);
            }

            LOGGER.info("Plugin carregado com sucesso!");

        } catch (Exception err) {
            LOGGER.error("Erro fatal ao carregar módulos do plugin!");
            err.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupInitialConfig(String role) throws Exception {
        String configFileName = role.equalsIgnoreCase("lobby")
                ? "lobby_config.yml"
                : "bukkit_config.yml";

        this.config = YamlDocument.create(
                new File(getDataFolder(), "config.yml"),
                Objects.requireNonNull(getClass().getResourceAsStream("/" + configFileName)),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setVersioning(new BasicVersioning("Config-Version"))
                        .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                        .build()
        );

        this.config.set("Server-Role", role);
        this.config.save();

        this.isConfigured = true;
    }

    private void syncBannedItems() {
        LOGGER.info("Sincronizando lista de itens banidos com a API NextFur...");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                APIResponse response = api.get("banitem");
                if (!response.isSuccessful()) {
                    LOGGER.error("Falha ao sincronizar itens banidos: resposta da API não foi bem-sucedida ("
                            + response.getStatusCode() + ")");
                    return;
                }

                String body = response.getBody();
                if (body == null || body.isEmpty()) {
                    LOGGER.warn("A API retornou corpo vazio para 'banitem'.");
                    return;
                }

                String[] itemsArray = gson.fromJson(body, String[].class);
                if (itemsArray == null || itemsArray.length == 0) {
                    LOGGER.warn("Nenhum item banido encontrado na resposta da API.");
                    return;
                }

                List<String> apiItems = List.of(itemsArray);

                bannedItems.clear();
                bannedItems.addAll(apiItems);

                LOGGER.info("Lista de itens banidos sincronizada com sucesso: " + apiItems.size() + " itens.");
            } catch (Exception e) {
                LOGGER.error("Erro ao sincronizar lista de itens banidos!");
                e.printStackTrace();
            }
        });
    }

    public YamlDocument _getConfig() {
        return this.config;
    }

    public List<String> getBannedItems() {
        return bannedItems;
    }

    public NextFurAPI getApi() {
        return this.api;
    }

    public FurWatchBukkit getPlugin() {
        return this.plugin;
    }

    public Logger _getLogger() {
        return this.LOGGER;
    }
}
