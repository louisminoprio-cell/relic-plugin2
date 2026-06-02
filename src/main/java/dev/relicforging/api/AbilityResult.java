package dev.relicforging.api;

/**
 * The outcome of attempting to fire a relic ability.
 *
 * Every ability method returns one of these so the command handler can give
 * the player precise, helpful feedback rather than silently doing nothing.
 */
public enum AbilityResult {
    /** Ability fired successfully. */
    SUCCESS,

    /** Player does not have enough Resonance Energy. */
    NOT_ENOUGH_ENERGY,

    /** Ability is still on cooldown. */
    ON_COOLDOWN,

    /** No relic is equipped. */
    NO_RELIC,

    /** Conditions for this ability aren't met (e.g. not in water for Tide). */
    CONDITIONS_NOT_MET,

    /** Something unexpected went wrong (log it and move on). */
    ERROR;
}
