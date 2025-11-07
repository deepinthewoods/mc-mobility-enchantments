package ninja.trek.mobility.physics;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;

/**
 * Custom physics helper for the Swooping enchantment.
 *
 * <p>The model intentionally ignores the player's head rotation and instead
 * derives all lift from the paper-plane style relationship:
 * {@code lift = speed^2 * liftCoefficient * angleOfAttackMultiplier}.
 * We assume the wing is always aligned with the current velocity vector, so
 * the angle-of-attack multiplier is fixed at {@code 1.0}.</p>
 */
public final class SwoopingPhysics {
    private static final Vec3d WORLD_UP = new Vec3d(0.0, 1.0, 0.0);
    private static final double EPSILON = 1.0e-6;

    private SwoopingPhysics() {
    }

    /**
     * Compute the per-tick velocity update for the Swooping enchantment.
     *
     * <p>Steps:
     * <ol>
     *     <li>Measure the current speed and derive lift magnitude from
     *     {@code speed^2 * SWOOPING_LIFT_COEFFICIENT}.</li>
     *     <li>Build a lift direction that is perpendicular to the velocity
     *     vector but biased upward as much as possible by projecting the world
     *     up axis onto the plane orthogonal to the velocity.</li>
     *     <li>Add gravity, add lift, then apply configurable drag so speeds
     *     stay bounded.</li>
     * </ol>
     * </p>
     */
    public static Vec3d computeGlideVelocity(LivingEntity entity, Vec3d oldVelocity, double gravity) {
        double speed = oldVelocity.length();

        Vec3d velocityAfterGravity = oldVelocity.add(0.0, -gravity, 0.0);

        if (speed < EPSILON) {
            return applyDrag(velocityAfterGravity);
        }

        Vec3d velocityDir = oldVelocity.normalize();
        Vec3d liftDirection = computeLiftDirection(velocityDir);

        double liftMagnitude = speed * speed * MobilityConfig.SWOOPING_LIFT_COEFFICIENT;
        Vec3d lift = liftDirection.multiply(liftMagnitude);

        Vec3d updatedVelocity = velocityAfterGravity.add(lift);
        return applyDrag(updatedVelocity);
    }

    private static Vec3d computeLiftDirection(Vec3d velocityDir) {
        Vec3d projectedUp = WORLD_UP.subtract(velocityDir.multiply(WORLD_UP.dotProduct(velocityDir)));

        if (projectedUp.lengthSquared() < EPSILON) {
            // Velocity is essentially vertical; choose an arbitrary horizontal perpendicular.
            Vec3d fallback = velocityDir.crossProduct(new Vec3d(1.0, 0.0, 0.0));
            if (fallback.lengthSquared() < EPSILON) {
                fallback = velocityDir.crossProduct(new Vec3d(0.0, 0.0, 1.0));
            }
            projectedUp = fallback;
        }

        return projectedUp.normalize();
    }

    private static Vec3d applyDrag(Vec3d velocity) {
        return velocity.multiply(MobilityConfig.SWOOPING_DRAG_XZ, MobilityConfig.SWOOPING_DRAG_Y, MobilityConfig.SWOOPING_DRAG_XZ);
    }
}
