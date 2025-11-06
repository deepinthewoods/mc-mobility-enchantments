package ninja.trek.mobility.state;

/**
 * Tracks the mobility state for a player.
 * This interface is implemented via mixin on ServerPlayerEntity.
 */
public interface MobilityState {

    /**
     * @return true if the player is currently swooping
     */
    boolean mobility$isSwooping();

    /**
     * Set whether the player is swooping
     */
    void mobility$setSwooping(boolean swooping);

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
     * @return true if the player is using the elytra enchantment
     */
    boolean mobility$isUsingElytraEnchantment();

    /**
     * Set whether the player is using the elytra enchantment
     */
    void mobility$setUsingElytraEnchantment(boolean using);

    /**
     * @return the number of ticks the elytra enchantment has been active
     */
    int mobility$getElytraTicks();

    /**
     * Set the number of ticks the elytra enchantment has been active
     */
    void mobility$setElytraTicks(int ticks);

    /**
     * @return the number of ticks the swooping enchantment has been active
     */
    int mobility$getSwoopingTicks();

    /**
     * Set the number of ticks the swooping enchantment has been active
     */
    void mobility$setSwoopingTicks(int ticks);

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
