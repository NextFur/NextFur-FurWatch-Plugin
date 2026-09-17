package net.nextfur.fws.bukkit.modules.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionPreset {
    private final String id;
    private final Pattern pattern;
    private final String description;
    private final String defaultMessage;
    private final String withTargetMessage;

    public ActionPreset(String id, String regex, String description, String defaultMessage, String withTargetMessage) {
        this.id = id;
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.description = description;
        this.defaultMessage = defaultMessage;
        this.withTargetMessage = withTargetMessage;
    }

    public String getId() {
        return id;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getWithTargetMessage() {
        return withTargetMessage;
    }

    /**
     * Checks if the raw input matches this preset and extracts target if captured.
     *
     * @param rawInput The input string following the command.
     * @return MatchResult if matched, or null if no match.
     */
    public MatchResult match(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return null;
        }

        String trimmed = rawInput.trim();

        // 1. Direct match by ID (e.g. "anger" or "anger Steve")
        String lowerTrimmed = trimmed.toLowerCase();
        String lowerId = id.toLowerCase();
        if (lowerTrimmed.equals(lowerId)) {
            return new MatchResult(this, null);
        }
        if (lowerTrimmed.startsWith(lowerId + " ")) {
            String target = trimmed.substring(id.length()).trim();
            return new MatchResult(this, target.isEmpty() ? null : target);
        }

        // 2. Regular expression match
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.matches()) {
            String target = null;
            if (matcher.groupCount() >= 1) {
                target = matcher.group(1);
                if (target != null) {
                    target = target.trim();
                    if (target.isEmpty()) {
                        target = null;
                    }
                }
            }
            return new MatchResult(this, target);
        }

        return null;
    }

    public static class MatchResult {
        private final ActionPreset preset;
        private final String target;

        public MatchResult(ActionPreset preset, String target) {
            this.preset = preset;
            this.target = target;
        }

        public ActionPreset getPreset() {
            return preset;
        }

        public String getTarget() {
            return target;
        }

        public String format(String playerName) {
            String template;
            if (target != null && !target.isEmpty() && preset.getWithTargetMessage() != null) {
                template = preset.getWithTargetMessage().replace("{target}", target).replace("%target%", target);
            } else {
                template = preset.getDefaultMessage();
            }
            return template.replace("%player%", playerName).replace("{player}", playerName);
        }
    }
}
