package net.nextfur.fws.bukkit.modules.death;

import java.util.UUID;

public class PlayerDeathData {
    private final UUID uuid;
    private String lastKnownName;
    private int deathCount;
    private long lastDeathTimestamp;
    private String lastDeathMessage;
    private String lastLocation;

    public PlayerDeathData(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.deathCount = 0;
        this.lastDeathTimestamp = 0L;
        this.lastDeathMessage = "";
        this.lastLocation = "";
    }

    public PlayerDeathData(UUID uuid, String lastKnownName, int deathCount, long lastDeathTimestamp, String lastDeathMessage, String lastLocation) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.deathCount = deathCount;
        this.lastDeathTimestamp = lastDeathTimestamp;
        this.lastDeathMessage = lastDeathMessage;
        this.lastLocation = lastLocation;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = Math.max(0, deathCount);
    }

    public int incrementDeathCount() {
        this.deathCount++;
        return this.deathCount;
    }

    public long getLastDeathTimestamp() {
        return lastDeathTimestamp;
    }

    public void setLastDeathTimestamp(long lastDeathTimestamp) {
        this.lastDeathTimestamp = lastDeathTimestamp;
    }

    public String getLastDeathMessage() {
        return lastDeathMessage;
    }

    public void setLastDeathMessage(String lastDeathMessage) {
        this.lastDeathMessage = lastDeathMessage;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }
}
