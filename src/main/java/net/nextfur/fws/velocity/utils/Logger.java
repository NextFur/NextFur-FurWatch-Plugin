package net.nextfur.fws.velocity.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public enum Level {
        INFO(NamedTextColor.GRAY),
        WARN(NamedTextColor.YELLOW),
        ERROR(NamedTextColor.RED),
        DEBUG(NamedTextColor.BLUE);

        private final NamedTextColor color;
        Level(NamedTextColor color) {
            this.color = color;
        }
        public NamedTextColor getColor() {
            return color;
        }
    }

    private final ProxyServer server;
    private final String prefix;
    private boolean debugEnabled;
    private boolean showTimestamp;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Logger(ProxyServer server, String prefix) {
        this.server = server;
        this.prefix = prefix;
        this.debugEnabled = false;
        this.showTimestamp = false;
    }

    public void setDebug(boolean enabled) {
        this.debugEnabled = enabled;
    }

    public void setTimestamps(boolean enabled) {
        this.showTimestamp = enabled;
    }

    public void log(Level level, String msg) {
        log(level, msg, true);
    }

    public void log(Level level, String msg, boolean usePrefix) {
        if (level == Level.DEBUG && !debugEnabled)
            return;

        StringBuilder sb = new StringBuilder();

        if (showTimestamp) {
            sb.append("[")
                    .append(TIME_FORMAT.format(LocalDateTime.now()))
                    .append("] ");
        }

        if (usePrefix)
            sb.append(prefix).append(" ");

        sb.append(level.name())
                .append(" > ")
                .append(msg);

        server.getConsoleCommandSource().sendMessage(
                Component.text(sb.toString(), level.getColor())
        );
    }

    public void info(String msg) { log(Level.INFO, msg); }
    public void warn(String msg) { log(Level.WARN, msg); }
    public void error(String msg) { log(Level.ERROR, msg); }
    public void debug(String msg) { log(Level.DEBUG, msg); }

    public void info(String msg, boolean usePrefix) { log(Level.INFO, msg, usePrefix); }
    public void warn(String msg, boolean usePrefix) { log(Level.WARN, msg, usePrefix); }
    public void error(String msg, boolean usePrefix) { log(Level.ERROR, msg, usePrefix); }
    public void debug(String msg, boolean usePrefix) { log(Level.DEBUG, msg, usePrefix); }
}
