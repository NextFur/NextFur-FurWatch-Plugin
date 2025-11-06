package net.nextfur.fws.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.nextfur.fws.api.APIResponse;
import net.nextfur.fws.api.NextFurAPI;
import net.nextfur.fws.velocity.utils.Logger;

import java.util.HashMap;

public class ConnectionListener {
    private final ProxyServer server;
    private final Logger logger;
    private final NextFurAPI api;

    public ConnectionListener(ProxyServer server, Logger logger, NextFurAPI api) {
        this.server = server;
        this.logger = logger;
        this.api = api;
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        Player player = event.getPlayer();

        String name = player.getUsername();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        HashMap<String, Object> data = new HashMap<>();

        data.put("username", name);
        data.put("ip", ip);

        APIResponse req = api.get("verify", data);
        if (req.isSuccessful() && !req.getBody().equalsIgnoreCase("ok")) {
            logger.warn("Bloqueando login não autorizado de " + name + " em " + ip);

            event.getPlayer().disconnect(MiniMessage.miniMessage().deserialize("<red>❌ IP não autorizado! A staff foi avisada!</red>"));
            api.postAsync("verify", data);
        } else if (!req.isSuccessful()) {
            event.getPlayer().disconnect(MiniMessage.miniMessage().deserialize("<red>❌ Erro de verificação! Caso este erro persista, abra um ticket em nosso discord!</red>"));
        }

        api.postAsync("p_ip", data);
    }
}
