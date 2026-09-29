package net.nextfur.fws.bukkit.modules.guard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GuardRegionTest {

    @Test
    public void testBoundingBoxContainment() {
        GuardRegion region = new GuardRegion("spawn", "world", 10.0, 60.0, 10.0, 20.0, 80.0, 20.0, GuardType.BLACKLIST);

        // Ponto interno
        assertTrue(region.contains(15.0, 70.0, 15.0));

        // Pontos de borda (inclusivos)
        assertTrue(region.contains(10.0, 60.0, 10.0));
        assertTrue(region.contains(20.0, 80.0, 20.0));

        // Pontos externos
        assertFalse(region.contains(9.9, 70.0, 15.0));
        assertFalse(region.contains(20.1, 70.0, 15.0));
        assertFalse(region.contains(15.0, 59.9, 15.0));
        assertFalse(region.contains(15.0, 80.1, 15.0));
        assertFalse(region.contains(15.0, 70.0, 9.9));
        assertFalse(region.contains(15.0, 70.0, 20.1));
    }

    @Test
    public void testGuardTypeParsing() {
        assertEquals(GuardType.BLACKLIST, GuardType.fromString("blacklist"));
        assertEquals(GuardType.BLACKLIST, GuardType.fromString("black"));
        assertEquals(GuardType.BLACKLIST, GuardType.fromString("bl"));
        assertEquals(GuardType.BLACKLIST, GuardType.fromString("deny"));

        assertEquals(GuardType.WHITELIST, GuardType.fromString("whitelist"));
        assertEquals(GuardType.WHITELIST, GuardType.fromString("white"));
        assertEquals(GuardType.WHITELIST, GuardType.fromString("wl"));
        assertEquals(GuardType.WHITELIST, GuardType.fromString("allow"));

        assertNull(GuardType.fromString("invalido"));
        assertNull(GuardType.fromString(null));
    }

    @Test
    public void testGuardFlagParsing() {
        assertEquals(GuardFlag.ENTRY, GuardFlag.fromString("entry"));
        assertEquals(GuardFlag.EXIT, GuardFlag.fromString("exit"));
        assertEquals(GuardFlag.BLOCK_BREAK, GuardFlag.fromString("block-break"));
        assertEquals(GuardFlag.BLOCK_BREAK, GuardFlag.fromString("block_break"));
        assertEquals(GuardFlag.BLOCK_BREAK, GuardFlag.fromString("break"));
        assertEquals(GuardFlag.BLOCK_PLACE, GuardFlag.fromString("block-place"));
        assertEquals(GuardFlag.BLOCK_PLACE, GuardFlag.fromString("place"));
        assertEquals(GuardFlag.INTERACT, GuardFlag.fromString("interact"));
        assertEquals(GuardFlag.PVP, GuardFlag.fromString("pvp"));

        assertNull(GuardFlag.fromString("desconhecido"));
    }

    @Test
    public void testDefaultFlagValues() {
        GuardRegion blacklistRegion = new GuardRegion("arena", "world", 0, 0, 0, 10, 10, 10, GuardType.BLACKLIST);
        // Em Blacklist: entrada negada, saída permitida, blocos negados, pvp permitido
        assertFalse(blacklistRegion.isFlagAllowed(GuardFlag.ENTRY));
        assertTrue(blacklistRegion.isFlagAllowed(GuardFlag.EXIT));
        assertFalse(blacklistRegion.isFlagAllowed(GuardFlag.BLOCK_BREAK));
        assertFalse(blacklistRegion.isFlagAllowed(GuardFlag.BLOCK_PLACE));
        assertFalse(blacklistRegion.isFlagAllowed(GuardFlag.INTERACT));
        assertTrue(blacklistRegion.isFlagAllowed(GuardFlag.PVP));

        GuardRegion whitelistRegion = new GuardRegion("quarentena", "world", 0, 0, 0, 10, 10, 10, GuardType.WHITELIST);
        // Em Whitelist: entrada permitida, saída negada
        assertTrue(whitelistRegion.isFlagAllowed(GuardFlag.ENTRY));
        assertFalse(whitelistRegion.isFlagAllowed(GuardFlag.EXIT));
    }

    @Test
    public void testFlagOverride() {
        GuardRegion region = new GuardRegion("vila", "world", 0, 0, 0, 10, 10, 10, GuardType.BLACKLIST);

        // Por padrão pvp é permitido
        assertTrue(region.isFlagAllowed(GuardFlag.PVP));

        // Força pvp a falso
        region.setFlag(GuardFlag.PVP, false);
        assertFalse(region.isFlagAllowed(GuardFlag.PVP));

        // Força block-break a verdadeiro
        assertFalse(region.isFlagAllowed(GuardFlag.BLOCK_BREAK));
        region.setFlag(GuardFlag.BLOCK_BREAK, true);
        assertTrue(region.isFlagAllowed(GuardFlag.BLOCK_BREAK));
    }

    @Test
    public void testJsonSerializationAndDeserialization() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        GuardRegion original = new GuardRegion("vip_zone", "world_nether", -50.0, 10.0, -50.0, 50.0, 120.0, 50.0, GuardType.BLACKLIST);
        original.setCustomMessage("&cApenas membros VIP podem entrar aqui!");
        original.setBypassPermission("furwatch.guard.vip");
        original.setFlag(GuardFlag.PVP, false);

        String json = gson.toJson(original);
        assertNotNull(json);

        GuardRegion deserialized = gson.fromJson(json, GuardRegion.class);
        assertNotNull(deserialized);
        deserialized.updateBoundingBox();

        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getWorldName(), deserialized.getWorldName());
        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getCustomMessage(), deserialized.getCustomMessage());
        assertEquals(original.getBypassPermission(), deserialized.getBypassPermission());
        assertFalse(deserialized.isFlagAllowed(GuardFlag.PVP));
        assertTrue(deserialized.contains(0.0, 64.0, 0.0));
        assertFalse(deserialized.contains(100.0, 64.0, 100.0));
    }
}
