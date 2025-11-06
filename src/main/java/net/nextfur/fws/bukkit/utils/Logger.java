package net.nextfur.fws.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public enum Level {
        INFO(ChatColor.GRAY),
        WARN(ChatColor.YELLOW),
        ERROR(ChatColor.RED),
        DEBUG(ChatColor.BLUE);

        private final ChatColor color;
        Level(ChatColor color) {
            this.color = color;
        }
        public ChatColor getColor() {
            return color;
        }
    }

    private final String prefix;
    private boolean debugEnabled;
    private boolean showTimestamp;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Logger(String prefix) {
        this.prefix = prefix + ChatColor.RESET;
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
            sb.append(ChatColor.DARK_GRAY)
                    .append("[")
                    .append(TIME_FORMAT.format(LocalDateTime.now()))
                    .append("] ");
        }

        if (usePrefix)
            sb.append(prefix);

        sb.append(level.getColor())
                .append(level.name())
                .append(ChatColor.DARK_GRAY)
                .append(" > ")
                .append(ChatColor.RESET)
                .append(msg);

        Bukkit.getConsoleSender().sendMessage(sb.toString());
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
