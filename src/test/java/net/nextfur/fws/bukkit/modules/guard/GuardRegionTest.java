package net.nextfur.fws.bukkit.modules.guard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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

    @Test
    public void testMemberManagement() {
        GuardRegion region = new GuardRegion("spawn", "world", 0, 0, 0, 10, 10, 10, GuardType.BLACKLIST);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        // Inicialmente sem membros
        assertTrue(region.getMembers().isEmpty());
        assertFalse(region.isMember(uuid1));
        assertFalse(region.isMember("Steve"));

        // Adiciona membro 1
        assertTrue(region.addMember(uuid1, "Steve"));
        assertTrue(region.isMember(uuid1));
        assertTrue(region.isMember("Steve"));
        assertTrue(region.isMember("steve")); // Case-insensitive
        assertEquals("Steve", region.getMemberNames().get(uuid1));

        // Adicionar mesmo UUID atualiza o nome e retorna false para novo elemento no set
        assertFalse(region.addMember(uuid1, "SteveUpdated"));
        assertEquals("SteveUpdated", region.getMemberNames().get(uuid1));

        // Adiciona membro 2
        assertTrue(region.addMember(uuid2, "Alex"));
        assertEquals(2, region.getMembers().size());

        // Remove por nome
        assertTrue(region.removeMember("alex"));
        assertFalse(region.isMember(uuid2));
        assertFalse(region.isMember("Alex"));
        assertEquals(1, region.getMembers().size());

        // Remove por UUID
        assertTrue(region.removeMember(uuid1));
        assertFalse(region.isMember(uuid1));
        assertTrue(region.getMembers().isEmpty());
    }

    @Test
    public void testClearMembers() {
        GuardRegion region = new GuardRegion("arena", "world", 0, 0, 0, 10, 10, 10, GuardType.BLACKLIST);
        region.addMember(UUID.randomUUID(), "Player1");
        region.addMember(UUID.randomUUID(), "Player2");
        assertEquals(2, region.getMembers().size());

        region.clearMembers();
        assertTrue(region.getMembers().isEmpty());
        assertTrue(region.getMemberNames().isEmpty());
    }

    @Test
    public void testJsonSerializationWithMembers() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        GuardRegion original = new GuardRegion("market", "world", 0, 0, 0, 50, 50, 50, GuardType.BLACKLIST);
        UUID uuid = UUID.randomUUID();
        original.addMember(uuid, "TraderBob");

        String json = gson.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains("TraderBob"));
        assertTrue(json.contains(uuid.toString()));

        GuardRegion deserialized = gson.fromJson(json, GuardRegion.class);
        assertNotNull(deserialized);
        assertTrue(deserialized.isMember(uuid));
        assertTrue(deserialized.isMember("TraderBob"));
        assertEquals("TraderBob", deserialized.getMemberNames().get(uuid));
    }

    @Test
    public void testBackwardCompatibilityJsonWithoutMembers() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonWithoutMembers = "{\n" +
                "  \"name\": \"old_region\",\n" +
                "  \"worldName\": \"world\",\n" +
                "  \"minX\": 0.0,\n" +
                "  \"minY\": 0.0,\n" +
                "  \"minZ\": 0.0,\n" +
                "  \"maxX\": 10.0,\n" +
                "  \"maxY\": 10.0,\n" +
                "  \"maxZ\": 10.0,\n" +
                "  \"type\": \"BLACKLIST\",\n" +
                "  \"flags\": {}\n" +
                "}";

        GuardRegion deserialized = gson.fromJson(jsonWithoutMembers, GuardRegion.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getMembers());
        assertNotNull(deserialized.getMemberNames());
        assertFalse(deserialized.isMember(UUID.randomUUID()));
        assertFalse(deserialized.isMember("Anyone"));

        UUID newUuid = UUID.randomUUID();
        assertTrue(deserialized.addMember(newUuid, "NewPlayer"));
        assertTrue(deserialized.isMember(newUuid));
        assertTrue(deserialized.isMember("NewPlayer"));
    }
}
