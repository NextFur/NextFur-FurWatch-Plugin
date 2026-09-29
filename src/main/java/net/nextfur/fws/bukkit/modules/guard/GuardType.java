package net.nextfur.fws.bukkit.modules.guard;

/**
 * Define o tipo de comportamento da barreira de uma região.
 */
public enum GuardType {
    /**
     * Região de Blacklist:
     * A entrada é proibida para jogadores sem bypass (área restrita / protegida).
     */
    BLACKLIST("Blacklist", "&cBlacklist (Entrada Restrita)"),

    /**
     * Região de Whitelist:
     * A saída é proibida para jogadores sem bypass (área de quarentena / arena / confinamento).
     */
    WHITELIST("Whitelist", "&aWhitelist (Saída Restrita)");

    private final String rawName;
    private final String formattedName;

    GuardType(String rawName, String formattedName) {
        this.rawName = rawName;
        this.formattedName = formattedName;
    }

    public String getRawName() {
        return rawName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    /**
     * Tenta identificar o tipo de região a partir de uma string.
     *
     * @param input Texto informado pelo usuário (ex: "blacklist", "white", "bl", "wl")
     * @return GuardType correspondente ou null se inválido.
     */
    public static GuardType fromString(String input) {
        if (input == null) return null;
        String normalized = input.trim().toLowerCase();
        return switch (normalized) {
            case "blacklist", "black", "bl", "deny", "restrito", "proibido" -> BLACKLIST;
            case "whitelist", "white", "wl", "allow", "permitido", "confinado" -> WHITELIST;
            default -> null;
        };
    }
}
