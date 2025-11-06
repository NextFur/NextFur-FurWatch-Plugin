package net.nextfur.fws.velocity;

import com.google.inject.Inject;

import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import net.nextfur.fws.api.NextFurAPI;
import net.nextfur.fws.velocity.listeners.ConnectionListener;
import net.nextfur.fws.velocity.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@Plugin(id = "furwatch", name = "FurWatch", version = "1.0", description = "Plugin Oficial NextFur", authors = {
        "Niix-Dan",
        "DiogoNSPI06"
})
public class FurWatchVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;
    private NextFurAPI api;

    private YamlDocument velocityConfig;

    @Inject
    public FurWatchVelocity(ProxyServer server, org.slf4j.Logger _logger, @DataDirectory Path dataFolder) {
        this.server = server;
        this.logger = new Logger(server, "FurWatch");
        this.dataFolder = dataFolder;

        try {
            velocityConfig = YamlDocument.create(
                    new File(dataFolder.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/velocity_config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("Config-Version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                            .build()
            );

            this.api = new NextFurAPI(velocityConfig.getString("FurGuard.ApiUrl"), velocityConfig.getString("FurGuard.ApiKey"));
        } catch (IOException ignored) {}
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new ConnectionListener(server, logger, api));
        logger.info("FurWatch Inicializado com sucesso!");
    }
}
