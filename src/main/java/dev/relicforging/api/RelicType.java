package dev.relicforging.api;

/**
 * Every possible relic archetype.
 * The enum value name is also used as the config key (lowercase) so
 * adding a new relic only requires: a new enum entry + config block + Relic subclass.
 */
public enum RelicType {
    GALE,
    EMBER,
    TIDE,
    ECHO,
    VANGUARD,
    BURROW,
    WARDEN,
    HOLLOW,
    PLAGUE;

    /** Returns the lowercase config key for this relic (e.g. "gale"). */
    public String configKey() {
        return name().toLowerCase();
    }
}
