package net.nextfur.fws.bukkit.modules.guard;

/**
 * Flags configuráveis para cada região de proteção.
 */
public enum GuardFlag {
    ENTRY("entry", "Entrada", "Controla se jogadores podem entrar na região"),
    EXIT("exit", "Saída", "Controla se jogadores podem sair da região"),
    BLOCK_BREAK("block-break", "Quebrar Blocos", "Controla se blocos podem ser quebrados"),
    BLOCK_PLACE("block-place", "Colocar Blocos", "Controla se blocos podem ser colocados"),
    INTERACT("interact", "Interação", "Controla o uso de baús, portas, alavancas e botões"),
    PVP("pvp", "PvP", "Controla o combate entre jogadores");

    private final String key;
    private final String displayName;
    private final String description;

    GuardFlag(String key, String displayName, String description) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Retorna o valor padrão da flag de acordo com o tipo de região.
     *
     * @param type Tipo de região (BLACKLIST ou WHITELIST)
     * @return true se permitido por padrão, false se negado
     */
    public boolean getDefaultValue(GuardType type) {
        return switch (this) {
            case ENTRY -> type == GuardType.WHITELIST;
            case EXIT -> type == GuardType.BLACKLIST;
            case BLOCK_BREAK, BLOCK_PLACE, INTERACT -> false;
            case PVP -> true;
        };
    }

    /**
     * Encontra a flag pelo nome ou aliases comuns.
     *
     * @param input Nome ou chave da flag
     * @return GuardFlag correspondente ou null
     */
    public static GuardFlag fromString(String input) {
        if (input == null) return null;
        String normalized = input.trim().toLowerCase().replace("_", "-");
        return switch (normalized) {
            case "entry", "entrar", "entrou" -> ENTRY;
            case "exit", "sair", "saida" -> EXIT;
            case "block-break", "blockbreak", "break", "quebrar", "minerar" -> BLOCK_BREAK;
            case "block-place", "blockplace", "place", "colocar", "construir" -> BLOCK_PLACE;
            case "interact", "usar", "interagir", "interacao" -> INTERACT;
            case "pvp", "combate", "luta" -> PVP;
            default -> null;
        };
    }
}
