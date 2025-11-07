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

    /** Lift multiplier for swooping enchantment (1.0 = normal lift, uses velocity direction) */
    public static final double SWOOPING_LIFT_MULTIPLIER = 1.0;

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
    /** Lift multiplier for elytra enchantment (1.0 = normal lift, same as vanilla elytra) */
    public static final double ELYTRA_LIFT_MULTIPLIER = 0.015;

    /** Drag multiplier for X/Z axes while gliding (vanilla default 0.99) */
    public static final double ELYTRA_DRAG_XZ = 0.99;

    /** Drag multiplier for Y axis while gliding (vanilla default 0.98) */
    public static final double ELYTRA_DRAG_Y = 0.98;

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
}
