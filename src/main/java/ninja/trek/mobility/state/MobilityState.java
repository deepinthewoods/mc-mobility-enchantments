package ninja.trek.mobility.state;

/**
 * Tracks the mobility state for a player.
 * This interface is implemented via mixin on ServerPlayerEntity.
 */
public interface MobilityState {

    /**
     * @return true if the player is gliding due to the elytra enchantment
     */
    boolean mobility$isElytraGliding();

    /**
     * Set whether the player is gliding due to the elytra enchantment
     */
    void mobility$setElytraGliding(boolean gliding);

    /**
     * @return the accumulated exhaustion that has not yet been applied while gliding
     */
    float mobility$getElytraHungerRemainder();

    /**
     * Store the accumulated exhaustion remainder while gliding
     */
    void mobility$setElytraHungerRemainder(float remainder);

    /**
     * @return true if the player is in wall jumping mode
     */
    boolean mobility$isWallJumping();

    /**
     * Set whether the player is in wall jumping mode
     */
    void mobility$setWallJumping(boolean wallJumping);

    /**
     * @return true if the player has used their double jump
     */
    boolean mobility$hasUsedDoubleJump();

    /**
     * Set whether the player has used their double jump
     */
    void mobility$setUsedDoubleJump(boolean used);

    /**
     * @return the cooldown remaining before another ability can be used
     */
    int mobility$getCooldown();

    /**
     * Set the cooldown ticks
     */
    void mobility$setCooldown(int ticks);

    /**
     * Reset all mobility states (called when player lands)
     */
    void mobility$resetStates();
}
