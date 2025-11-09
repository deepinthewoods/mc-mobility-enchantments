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

    /** Lift coefficient for swooping enchantment (used in speed^2 * coeff formula) */
    public static final double SWOOPING_LIFT_COEFFICIENT = 0.2;

    /** Degrees to bias the lift vector toward world-up (0 = no bias, 90 = fully upright) */
    public static final double SWOOPING_LIFT_UPWARD_BIAS_DEGREES = 3.0;

    /** Horizontal drag applied after swooping physics step (1.0 = no drag) */
    public static final double SWOOPING_DRAG_XZ = 0.99988;

    /** Vertical drag applied after swooping physics step (1.0 = no drag) */
    public static final double SWOOPING_DRAG_Y = 0.9805;

    /** Horizontal impulse applied when swooping starts to kick the player forward */
    public static final double SWOOPING_START_IMPULSE = 0.25;

    /** Maximum movement speed while swooping (<= 0 disables clamping) */
    public static final double SWOOPING_SPEED_LIMIT = 1.0;

    /** Degrees from straight up where swooping forces are disabled */
    public static final double SWOOPING_DEAD_ZONE_DEGREES = 30.0;

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
