package net.nextfur.fws.bukkit.modules.guard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.*;

/**
 * Representa uma região protegida com limites cubóides (BoundingBox).
 */
public class GuardRegion {
    private String name;
    private String worldName;
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;
    private GuardType type;
    private Map<GuardFlag, Boolean> flags;
    private Set<UUID> members;
    private Map<UUID, String> memberNames;
    private String customMessage;
    private String bypassPermission;
    private int priority;
    private long createdAt;

    private transient BoundingBox boundingBox;

    public GuardRegion() {
        this.flags = new EnumMap<>(GuardFlag.class);
        this.members = new HashSet<>();
        this.memberNames = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.priority = 0;
    }

    public GuardRegion(String name, String worldName, double minX, double minY, double minZ,
                       double maxX, double maxY, double maxZ, GuardType type) {
        this();
        this.name = name.toLowerCase().trim();
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.type = type != null ? type : GuardType.BLACKLIST;
        updateBoundingBox();
    }

    /**
     * Cria uma região cobrindo completamente os blocos selecionados por pos1 e pos2.
     */
    public static GuardRegion fromLocations(String name, Location p1, Location p2, GuardType type) {
        if (p1 == null || p2 == null || p1.getWorld() == null) {
            throw new IllegalArgumentException("As localizações e o mundo não podem ser nulos.");
        }

        String worldName = p1.getWorld().getName();
        double minX = Math.min(p1.getBlockX(), p2.getBlockX());
        double minY = Math.min(p1.getBlockY(), p2.getBlockY());
        double minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        double maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + 1.0;
        double maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + 1.0;
        double maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + 1.0;

        return new GuardRegion(name, worldName, minX, minY, minZ, maxX, maxY, maxZ, type);
    }

    /**
     * Atualiza a instância nativa de BoundingBox em memória.
     */
    public void updateBoundingBox() {
        this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Verifica se uma dada localização está dentro desta região.
     */
    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(this.worldName)) return false;
        return contains(loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Verifica se as coordenadas estão dentro dos limites desta região (limites inclusivos).
     */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Retorna se a flag especificada está permitida nesta região.
     */
    public boolean isFlagAllowed(GuardFlag flag) {
        if (flags != null && flags.containsKey(flag)) {
            return flags.get(flag);
        }
        return flag.getDefaultValue(this.type);
    }

    /**
     * Define o valor de uma flag específica.
     */
    public void setFlag(GuardFlag flag, boolean allowed) {
        if (this.flags == null) {
            this.flags = new EnumMap<>(GuardFlag.class);
        }
        this.flags.put(flag, allowed);
    }

    /**
     * Retorna a localização central da região (ideal para teleporte).
     */
    public Location getCenter() {
        World world = Bukkit.getWorld(this.worldName);
        if (world == null) return null;
        double cx = (minX + maxX) / 2.0;
        double cy = minY + 1.0; // Um bloco acima do chão da região
        double cz = (minZ + maxZ) / 2.0;
        return new Location(world, cx, cy, cz);
    }

    /**
     * Retorna o volume total aproximado em blocos.
     */
    public long getVolume() {
        long dx = Math.round(maxX - minX);
        long dy = Math.round(maxY - minY);
        long dz = Math.round(maxZ - minZ);
        return Math.max(1L, dx) * Math.max(1L, dy) * Math.max(1L, dz);
    }

    // Getters e Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.toLowerCase().trim() : null;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public GuardType getType() {
        return type;
    }

    public void setType(GuardType type) {
        this.type = type;
    }

    public Map<GuardFlag, Boolean> getFlags() {
        if (this.flags == null) {
            this.flags = new EnumMap<>(GuardFlag.class);
        }
        return flags;
    }

    public void setFlags(Map<GuardFlag, Boolean> flags) {
        this.flags = flags;
    }

    public Set<UUID> getMembers() {
        if (this.members == null) {
            this.members = new HashSet<>();
        }
        return this.members;
    }

    public void setMembers(Set<UUID> members) {
        this.members = members;
    }

    public Map<UUID, String> getMemberNames() {
        if (this.memberNames == null) {
            this.memberNames = new HashMap<>();
        }
        return this.memberNames;
    }

    public void setMemberNames(Map<UUID, String> memberNames) {
        this.memberNames = memberNames;
    }

    /**
     * Adiciona um jogador como membro desta região.
     */
    public boolean addMember(UUID uuid, String name) {
        if (uuid == null) return false;
        if (this.members == null) {
            this.members = new HashSet<>();
        }
        if (this.memberNames == null) {
            this.memberNames = new HashMap<>();
        }
        boolean added = this.members.add(uuid);
        if (name != null && !name.trim().isEmpty()) {
            this.memberNames.put(uuid, name.trim());
        }
        return added;
    }

    /**
     * Remove um jogador por UUID da lista de membros desta região.
     */
    public boolean removeMember(UUID uuid) {
        if (uuid == null || this.members == null) return false;
        if (this.memberNames != null) {
            this.memberNames.remove(uuid);
        }
        return this.members.remove(uuid);
    }

    /**
     * Remove um jogador por nome (ignorando maiúsculas/minúsculas).
     */
    public boolean removeMember(String name) {
        if (name == null || this.members == null) return false;
        UUID found = null;
        if (this.memberNames != null) {
            for (Map.Entry<UUID, String> entry : this.memberNames.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(name.trim())) {
                    found = entry.getKey();
                    break;
                }
            }
        }
        if (found != null) {
            return removeMember(found);
        }
        return false;
    }

    /**
     * Verifica se o UUID é membro desta região.
     */
    public boolean isMember(UUID uuid) {
        if (uuid == null || this.members == null) return false;
        return this.members.contains(uuid);
    }

    /**
     * Verifica se o nome do jogador é membro desta região.
     */
    public boolean isMember(String name) {
        if (name == null || this.memberNames == null) return false;
        for (String memberName : this.memberNames.values()) {
            if (name.equalsIgnoreCase(memberName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Limpa todos os membros da região.
     */
    public void clearMembers() {
        if (this.members != null) {
            this.members.clear();
        }
        if (this.memberNames != null) {
            this.memberNames.clear();
        }
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public void setBypassPermission(String bypassPermission) {
        this.bypassPermission = bypassPermission;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public BoundingBox getBoundingBox() {
        if (boundingBox == null) {
            updateBoundingBox();
        }
        return boundingBox;
    }
}
