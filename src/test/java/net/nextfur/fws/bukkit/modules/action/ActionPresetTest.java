package net.nextfur.fws.bukkit.modules.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ActionPresetTest {

    private final Map<String, ActionPreset> presets = new LinkedHashMap<>();

    @BeforeEach
    public void setup() {
        presets.clear();

        // Register default presets
        register(new ActionPreset("anger",
                "^(?:anger|raiva|irritad[oa]|brav[oa]|braveza)(?:\\s+(.+))?$",
                "Expressa raiva ou irritação",
                "&e* &6%player% &cfica com raiva! &e*",
                "&e* &6%player% &cfica com raiva de &f{target}&c! &e*"));

        register(new ActionPreset("happy",
                "^(?:happy|feliz|felicidade|alegre|alegria)(?:\\s+(.+))?$",
                "Expressa alegria ou felicidade",
                "&e* &6%player% &aestá muito feliz! &e*",
                "&e* &6%player% &afica feliz com &f{target}&a! &e*"));

        register(new ActionPreset("sad",
                "^(?:sad|triste|tristeza|deprimid[oa]|chorar|cry)(?:\\s+(.+))?$",
                "Expressa tristeza ou choro",
                "&e* &6%player% &9está triste e começa a chorar... &e*",
                "&e* &6%player% &9chora no ombro de &f{target}&9... &e*"));

        register(new ActionPreset("laugh",
                "^(?:laugh|rir|risad[aa]|gargalhad[aa]|haha+|lol)(?:\\s+(.+))?$",
                "Solta uma risada divertida",
                "&e* &6%player% &ecomeça a rir alegremente! &e*",
                "&e* &6%player% &eri junto com &f{target}&e! &e*"));

        register(new ActionPreset("hug",
                "^(?:hug|abraco|abraço|abracar|abraçar)(?:\\s+(.+))?$",
                "Dá ou pede um abraço",
                "&e* &6%player% &dquer um abraço quentinho. &e*",
                "&e* &6%player% &ddá um abraço carinhoso em &f{target}&d! &e*"));

        register(new ActionPreset("wave",
                "^(?:wave|acenar|aceno|tchau|ola|olá|oi)(?:\\s+(.+))?$",
                "Acena amigavelmente com a mão",
                "&e* &6%player% &eacena amigavelmente com a mão. &e*",
                "&e* &6%player% &eacena amigavelmente para &f{target}&e! &e*"));

        register(new ActionPreset("blush",
                "^(?:blush|corar|corad[oa]|vergonha)(?:\\s+(.+))?$",
                "Fica corado(a) de vergonha",
                "&e* &6%player% &dfica corado(a) e com vergonha... &e*",
                "&e* &6%player% &dfica corado(a) ao olhar para &f{target}&d... &e*"));

        register(new ActionPreset("scared",
                "^(?:scared|medo|assustad[oa]|temer)(?:\\s+(.+))?$",
                "Expressa medo ou susto",
                "&e* &6%player% &ctreme de medo e fica assustado(a)! &e*",
                "&e* &6%player% &cesconde-se atrás de &f{target}&c com medo! &e*"));

        register(new ActionPreset("tired",
                "^(?:tired|cansad[oa]|sono|bocejar|sleepy)(?:\\s+(.+))?$",
                "Expressa cansaço ou sono",
                "&e* &6%player% &7boceja com sono e aparenta cansaço. &e*",
                "&e* &6%player% &7apoia a cabeça em &f{target}&7 de tanto sono. &e*"));

        register(new ActionPreset("confused",
                "^(?:confused|confus[oa]|duvida|dúvida)(?:\\s+(.+))?$",
                "Fica confuso(a) com algo",
                "&e* &6%player% &eolha em volta confuso(a) e coça a cabeça. &e*",
                "&e* &6%player% &eolha para &f{target}&e sem entender nada. &e*"));

        register(new ActionPreset("dance",
                "^(?:dance|dancar|dançar|danca|dança)(?:\\s+(.+))?$",
                "Começa a dançar animadamente",
                "&e* &6%player% &bcomeça a dançar alegremente! &e*",
                "&e* &6%player% &bpuxa &f{target}&b para uma dança animada! &e*"));

        register(new ActionPreset("clap",
                "^(?:clap|aplaudir|palmas|aplauso)(?:\\s+(.+))?$",
                "Bate palmas entusiasticamente",
                "&e* &6%player% &ebate palmas calorosamente! &e*",
                "&e* &6%player% &eaplaude calorosamente a atitude de &f{target}&e! &e*"));

        register(new ActionPreset("sit",
                "^(?:sit|sentar|sentad[oa]|descansar)(?:\\s+(.+))?$",
                "Senta-se no chão para descansar",
                "&e* &6%player% &7senta-se no chão para descansar. &e*",
                "&e* &6%player% &7senta-se ao lado de &f{target}&7. &e*"));

        register(new ActionPreset("think",
                "^(?:think|pensar|pensativ[oa]|refletir)(?:\\s+(.+))?$",
                "Fica pensativo(a) refletindo",
                "&e* &6%player% &7fica pensativo(a), refletindo em silêncio... &e*",
                "&e* &6%player% &7pensa profundamente sobre o que &f{target}&7 disse. &e*"));

        register(new ActionPreset("wink",
                "^(?:wink|piscar|piscadela|piscada)(?:\\s+(.+))?$",
                "Dá uma piscadinha charmosa",
                "&e* &6%player% &6dá uma piscadinha charmosa. &e*",
                "&e* &6%player% &6dá uma piscadinha charmosa para &f{target}&6. &e*"));
    }

    private void register(ActionPreset preset) {
        presets.put(preset.getId(), preset);
    }

    private ActionPreset.MatchResult matchFirst(String input) {
        for (ActionPreset p : presets.values()) {
            ActionPreset.MatchResult res = p.match(input);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    @Test
    public void testMin10PresetsExist() {
        assertTrue(presets.size() >= 10, "Deve haver pelo menos 10 presets implementados");
        assertEquals(15, presets.size());
    }

    @Test
    public void testAngerPresetDirectAndRegex() {
        ActionPreset.MatchResult m1 = matchFirst("anger");
        assertNotNull(m1);
        assertEquals("anger", m1.getPreset().getId());
        assertNull(m1.getTarget());
        assertEquals("&e* &6Steve &cfica com raiva! &e*", m1.format("Steve"));

        // With target
        ActionPreset.MatchResult m2 = matchFirst("anger Alex");
        assertNotNull(m2);
        assertEquals("Alex", m2.getTarget());
        assertEquals("&e* &6Steve &cfica com raiva de &fAlex&c! &e*", m2.format("Steve"));

        // Regex variation: raiva
        ActionPreset.MatchResult m3 = matchFirst("raiva");
        assertNotNull(m3);
        assertEquals("&e* &6Steve &cfica com raiva! &e*", m3.format("Steve"));

        // Regex variation: irritado com target
        ActionPreset.MatchResult m4 = matchFirst("irritado Herobrine");
        assertNotNull(m4);
        assertEquals("Herobrine", m4.getTarget());
        assertEquals("&e* &6Steve &cfica com raiva de &fHerobrine&c! &e*", m4.format("Steve"));
    }

    @Test
    public void testHappyPresetDirectAndRegex() {
        ActionPreset.MatchResult m1 = matchFirst("happy");
        assertNotNull(m1);
        assertEquals("happy", m1.getPreset().getId());
        assertNull(m1.getTarget());
        assertEquals("&e* &6Steve &aestá muito feliz! &e*", m1.format("Steve"));

        // Regex variation: feliz
        ActionPreset.MatchResult m2 = matchFirst("feliz");
        assertNotNull(m2);
        assertEquals("&e* &6Steve &aestá muito feliz! &e*", m2.format("Steve"));

        // Regex variation with target: alegre
        ActionPreset.MatchResult m3 = matchFirst("alegre Alex");
        assertNotNull(m3);
        assertEquals("Alex", m3.getTarget());
        assertEquals("&e* &6Steve &afica feliz com &fAlex&a! &e*", m3.format("Steve"));
    }

    @Test
    public void testHugAndWavePresets() {
        ActionPreset.MatchResult hugTarget = matchFirst("hug Alex");
        assertNotNull(hugTarget);
        assertEquals("hug", hugTarget.getPreset().getId());
        assertEquals("Alex", hugTarget.getTarget());
        assertEquals("&e* &6Steve &ddá um abraço carinhoso em &fAlex&d! &e*", hugTarget.format("Steve"));

        ActionPreset.MatchResult abraçarTarget = matchFirst("abraçar Alex");
        assertNotNull(abraçarTarget);
        assertEquals("Alex", abraçarTarget.getTarget());

        ActionPreset.MatchResult wave = matchFirst("acenar Notch");
        assertNotNull(wave);
        assertEquals("wave", wave.getPreset().getId());
        assertEquals("Notch", wave.getTarget());
        assertEquals("&e* &6Steve &eacena amigavelmente para &fNotch&e! &e*", wave.format("Steve"));
    }

    @Test
    public void testCustomActionNotMatched() {
        ActionPreset.MatchResult custom = matchFirst("olha para o horizonte pensativo sob a luz da lua");
        assertNull(custom, "Ação livre arbitrária não deve colidir com presets");
    }
}
