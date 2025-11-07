package ninja.trek.mobility.physics;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;

/**
 * Copy of vanilla's glide velocity calculation with a couple of configurable tweaks.
 * Instead of patching the original method via mixin, the mobility mod calls into this
 * helper whenever a player is gliding because of the Elytra enchantment.
 *
 * <p>This class intentionally mirrors Mojang's implementation so that upstream behaviour
 * stays familiar while still letting {@link MobilityConfig} expose balances knobs.
 * Additional documentation is provided throughout to make the aerodynamic reasoning
 * explicit, which is otherwise scattered through trigonometric one-liners in the
 * original code.</p>
 */
public final class ElytraPhysics {
    private ElytraPhysics() {
    }

    /**
     * Fully simulates the Elytra's per-tick velocity update.
     *
     * <p>The physics works in three conceptual phases:
     * <ol>
     *     <li>Start with the entity's previous velocity and immediately apply gravity
     *         plus a lift term that depends on pitch and the optional mobility multiplier.</li>
     *     <li>Modify the vertical velocity based on how aggressively the player is pointing
     *         their pitch into or away from the ground. Diving converts potential energy
     *         into horizontal speed, while shallow climbs trade speed for altitude.</li>
     *     <li>Finally, damp the motion toward the entity's look vector so that steering
     *         feels responsive. This keeps gliding stable instead of oscillating.</li>
     * </ol>
     * The result is returned with Mojang's original drag multipliers (0.99, 0.98, 0.99)
     * applied to the three axes.</p>
     *
     * @param entity       player or mob that is currently gliding; supplies orientation vectors
     * @param oldVelocity  velocity at the start of the tick, before Elytra adjustments
     * @param gravity      pre-computed gravity acceleration supplied by the caller
     * @return the new velocity vector that should be assigned to the entity for this tick
     */
    public static Vec3d computeGlideVelocity(LivingEntity entity, Vec3d oldVelocity, double gravity) {
        // Unit vector for the direction the entity is currently looking.
        Vec3d rotation = entity.getRotationVector();

        // Convert pitch to radians because Minecraft stores it in degrees on the entity.
        float pitchRadians = entity.getPitch() * (float) (Math.PI / 180.0);

        // Combined magnitude of the horizontal look direction components (ignore Y).
        double horizontalRotation = Math.sqrt(rotation.x * rotation.x + rotation.z * rotation.z);

        // Current planar speed; governs how much momentum can be redistributed.
        double horizontalSpeed = oldVelocity.horizontalLength();

        // Factor reused by several terms; describes how "flat" the Elytra is aligned.
        double cosSquared = MathHelper.square(Math.cos(pitchRadians));

        double liftMultiplier = MobilityConfig.ELYTRA_LIFT_MULTIPLIER;

        // Start from the old velocity and apply both gravity and Elytra lift.
        Vec3d velocity = oldVelocity.add(0.0, gravity * (-1.0 + cosSquared * 0.75 * liftMultiplier), 0.0);

        if (velocity.y < 0.0 && horizontalRotation > 0.0) {
            // Diving with a horizontal look vector generates aerodynamic lift that tempers descent.
            double adjust = velocity.y * -0.1 * cosSquared;
            velocity = velocity.add(rotation.x * adjust / horizontalRotation, adjust, rotation.z * adjust / horizontalRotation);
        }

        if (pitchRadians < 0.0F && horizontalRotation > 0.0) {
            // Pulling the pitch below the horizon trades horizontal momentum for a strong upward push.
            double adjust = horizontalSpeed * -MathHelper.sin(pitchRadians) * 0.04;
            velocity = velocity.add(-rotation.x * adjust / horizontalRotation, adjust * 3.2, -rotation.z * adjust / horizontalRotation);
        }

        if (horizontalRotation > 0.0) {
            // Nudge the final motion toward the direction the player is facing to keep steering responsive.
            double adjustX = (rotation.x / horizontalRotation * horizontalSpeed - velocity.x) * 0.1;
            double adjustZ = (rotation.z / horizontalRotation * horizontalSpeed - velocity.z) * 0.1;
            velocity = velocity.add(adjustX, 0.0, adjustZ);
        }

        // Apply vanilla drag so Elytra flight does not slowly accelerate forever.
        return velocity.multiply(MobilityConfig.ELYTRA_DRAG_XZ, MobilityConfig.ELYTRA_DRAG_Y, MobilityConfig.ELYTRA_DRAG_XZ);
    }
}
