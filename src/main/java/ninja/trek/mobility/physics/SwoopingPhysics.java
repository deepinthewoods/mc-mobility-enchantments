package ninja.trek.mobility.physics;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
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
        if (isWithinDeadZone(velocityDir)) {
            return clampSpeed(velocityAfterGravity);
        }

        Vec3d liftDirection = computeLiftDirection(velocityDir);

        double liftMagnitude = speed * speed * MobilityConfig.SWOOPING_LIFT_COEFFICIENT;
        Vec3d lift = liftDirection.multiply(liftMagnitude);

        Vec3d updatedVelocity = velocityAfterGravity.add(lift);
        Vec3d draggedVelocity = applyDrag(updatedVelocity);
        return clampSpeed(draggedVelocity);
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

        Vec3d normalized = projectedUp.normalize();
        if (normalized.dotProduct(WORLD_UP) < 0.0D) {
            normalized = normalized.multiply(-1.0D);
        }
        return applyUpwardBias(normalized);
    }

    private static Vec3d applyDrag(Vec3d velocity) {
        return velocity.multiply(MobilityConfig.SWOOPING_DRAG_XZ, MobilityConfig.SWOOPING_DRAG_Y, MobilityConfig.SWOOPING_DRAG_XZ);
    }

    private static Vec3d applyUpwardBias(Vec3d liftDirection) {
        double biasDegrees = MobilityConfig.SWOOPING_LIFT_UPWARD_BIAS_DEGREES;
        if (biasDegrees <= 0.0D) {
            return liftDirection;
        }

        double t = MathHelper.clamp(biasDegrees / 90.0D, 0.0D, 1.0D);
        Vec3d biased = liftDirection.multiply(1.0D - t).add(WORLD_UP.multiply(t));
        if (biased.lengthSquared() < EPSILON) {
            return liftDirection;
        }
        return biased.normalize();
    }

    private static Vec3d clampSpeed(Vec3d velocity) {
        double limit = MobilityConfig.SWOOPING_SPEED_LIMIT;
        if (limit <= 0.0D) {
            return velocity;
        }

        double speedSquared = velocity.lengthSquared();
        double limitSquared = limit * limit;
        if (speedSquared <= limitSquared) {
            return velocity;
        }

        double scale = limit / Math.sqrt(speedSquared);
        return new Vec3d(velocity.x * scale, velocity.y * scale, velocity.z * scale);
    }

    private static boolean isWithinDeadZone(Vec3d velocityDir) {
        double deadZoneDegrees = MobilityConfig.SWOOPING_DEAD_ZONE_DEGREES;
        if (deadZoneDegrees <= 0.0D) {
            return false;
        }

        double dot = velocityDir.dotProduct(WORLD_UP);
        if (dot <= 0.0D) {
            return false;
        }

        double radians = deadZoneDegrees * MathHelper.RADIANS_PER_DEGREE;
        double cosThreshold = Math.cos(radians);
        return dot >= cosThreshold;
    }
}
