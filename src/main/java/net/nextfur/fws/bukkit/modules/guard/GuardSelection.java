package net.nextfur.fws.bukkit.modules.guard;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Armazena a seleção temporária de coordenadas pos1 e pos2 de um jogador
 * antes de criar ou redimensionar uma região.
 */
public class GuardSelection {
    private Location pos1;
    private Location pos2;

    public GuardSelection() {}

    public GuardSelection(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1 != null ? pos1.clone() : null;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2 != null ? pos2.clone() : null;
    }

    /**
     * Verifica se ambos os pontos foram definidos e pertencem ao mesmo mundo.
     */
    public boolean isComplete() {
        return pos1 != null && pos2 != null 
                && pos1.getWorld() != null 
                && pos2.getWorld() != null 
                && pos1.getWorld().getName().equals(pos2.getWorld().getName());
    }

    public World getWorld() {
        if (pos1 != null && pos1.getWorld() != null) {
            return pos1.getWorld();
        }
        if (pos2 != null && pos2.getWorld() != null) {
            return pos2.getWorld();
        }
        return null;
    }

    /**
     * Expande verticalmente a seleção para cobrir desde a camada mais profunda até o topo do mundo.
     *
     * @return true se a expansão foi bem sucedida, false se pos1 ou pos2 não estavam definidos.
     */
    public boolean expandVert() {
        if (pos1 == null || pos2 == null || pos1.getWorld() == null) {
            return false;
        }

        World world = pos1.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        pos1.setY(minY);
        pos2.setY(maxY);
        return true;
    }

    /**
     * Calcula o volume total de blocos da seleção cubóide.
     */
    public long getVolume() {
        if (!isComplete()) {
            return 0;
        }

        long dx = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1L;
        long dy = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1L;
        long dz = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1L;
        return dx * dy * dz;
    }

    public void clear() {
        this.pos1 = null;
        this.pos2 = null;
    }
}
