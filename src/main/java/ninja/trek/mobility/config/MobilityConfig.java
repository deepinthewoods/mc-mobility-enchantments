package ninja.trek.mobility.config;

/**
 * Configuration constants for mobility enchantments.
 * All values can be adjusted for balance tuning.
 */
public class MobilityConfig {

    // === SWOOPING ===
    /** Hunger consumed per second while swooping (in half-drumsticks, 0.1 = 0.05 drumsticks) */
    public static final float SWOOPING_HUNGER_PER_SECOND = 0.1f;

    /** Force applied when player moves horizontally while swooping (same as falling normally) */
    public static final float SWOOPING_AIR_CONTROL = 0.02f;

    // === DASH ===
    /** Velocity magnitude for dash (sufficient to move ~10 blocks) */
    public static final double DASH_VELOCITY = 1.5;

    /** Hunger consumed per dash use (in half-drumsticks, 2 = 1 drumstick) */
    public static final int DASH_HUNGER_COST = 2;

    // === DOUBLE JUMP ===
    /** Hunger consumed per double jump (in half-drumsticks, 2 = 1 drumstick) */
    public static final int DOUBLE_JUMP_HUNGER_COST = 2;

    /** Velocity applied on double jump (same as normal jump) */
    public static final double DOUBLE_JUMP_VELOCITY = 0.42;

    // === ELYTRA ===
    /** Lift multiplier for elytra enchantment (0.5 = half normal lift) */
    public static final double ELYTRA_LIFT_MULTIPLIER = 0.5;

    /** Hunger consumed per 15 seconds of elytra use (in half-drumsticks) */
    public static final int ELYTRA_HUNGER_PER_15S = 1;

    /** Ticks between hunger consumption (15 seconds = 300 ticks) */
    public static final int ELYTRA_HUNGER_TICK_INTERVAL = 300;

    // === WALL JUMP ===
    /** Hunger consumed per wall jump (in half-drumsticks, 0.5 = 0.25 drumsticks) */
    public static final float WALL_JUMP_HUNGER_COST = 0.5f;

    /** Velocity magnitude when wall jumping */
    public static final double WALL_JUMP_VELOCITY = 0.6;

    /** Distance from player hitbox to check for walls (in blocks) */
    public static final double WALL_DETECTION_DISTANCE = 0.0625; // 1/16 block

    /** Air control force when in wall jumping mode (replaces normal air movement) */
    public static final float WALL_JUMP_AIR_CONTROL = 0.02f;

    /** Speed limit multiplier when in wall jumping mode */
    public static final double WALL_JUMP_SPEED_LIMIT = 1.0;

    // === GENERAL ===
    /** Cooldown ticks to prevent ability spam (3 ticks = 0.15 seconds) */
    public static final int ABILITY_COOLDOWN_TICKS = 3;

    // === HAUNCHES ===
    /** Initial number of haunches when an enchanted item is created/found */
    public static final int INITIAL_HAUNCHES = 10;

    /** Maximum number of haunches an item can hold */
    public static final int MAX_HAUNCHES = 20;

    /** Haunches consumed per double jump */
    public static final int DOUBLE_JUMP_HAUNCH_COST = 1;

    /** Haunches consumed per dash */
    public static final int DASH_HAUNCH_COST = 1;

    /** Haunches consumed per tick while swooping (0.1 = consumes 1 haunch every 10 ticks) */
    public static final float SWOOPING_HAUNCH_PER_TICK = 0.05f;

    /** Haunches consumed per tick while using elytra (consumed over 15 seconds = 300 ticks) */
    public static final float ELYTRA_HAUNCH_PER_TICK = 1.0f / 300.0f;

    /** Haunches consumed per wall jump */
    public static final int WALL_JUMP_HAUNCH_COST = 1;
}
