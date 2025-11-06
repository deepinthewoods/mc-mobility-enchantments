package ninja.trek.mobility.mixin;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.state.MobilityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle player movement modifications for mobility enchantments.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    private PlayerEntity self() {
        return (PlayerEntity) (Object) this;
    }

    private double getEffectiveGravity() {
        return ((LivingEntityAccessor) this).invokeGetEffectiveGravity();
    }

    /**
     * Inject into travel method to handle custom air movement for elytra, wall jumping, and swooping.
     * IMPORTANT: This must be cancellable for elytra to work properly!
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = self();

        // Only handle server-side (client will receive velocity updates from server)
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            // If client is gliding, cancel vanilla elytra physics
            // Server will handle all physics and sync velocity
            // Check using the FALL_FLYING flag (entity flag 7)
            if (((EntityAccessor) player).invokeGetFlag(7)) {
                ci.cancel();
            }
            return;
        }

        MobilityState state = (MobilityState) serverPlayer;

        // Handle elytra enchantment - MUST cancel vanilla travel
        if (state.mobility$isUsingElytraEnchantment() && !player.isOnGround()) {
            handleElytraMovement(serverPlayer, state, movementInput);
            ci.cancel(); // Cancel vanilla travel - we handled everything
            return;
        }

        // Handle swooping enchantment - MUST cancel vanilla travel
        if (state.mobility$isSwooping() && !player.isOnGround()) {
            handleSwoopingMovement(serverPlayer, state, movementInput);
            ci.cancel(); // Cancel vanilla travel - we handled everything
            return;
        }

        // Handle wall jumping air control (doesn't cancel - modifies before vanilla runs)
        if (state.mobility$isWallJumping() && !player.isOnGround()) {
            handleWallJumpingMovement(serverPlayer, movementInput);
        }
    }

    private void handleWallJumpingMovement(ServerPlayerEntity player, Vec3d movementInput) {
        // Apply horizontal force based on movement input
        // This replaces normal air movement
        if (movementInput.lengthSquared() > 0) {
            Vec3d velocity = player.getVelocity();

            // Get player's yaw to determine movement direction
            float yaw = player.getYaw();
            double yawRad = Math.toRadians(yaw);

            // Calculate movement direction in world space
            double forward = movementInput.z;
            double strafe = movementInput.x;

            double moveX = -Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe;
            double moveZ = Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe;

            // Apply force
            Vec3d force = new Vec3d(moveX, 0, moveZ).normalize().multiply(MobilityConfig.WALL_JUMP_AIR_CONTROL);
            player.setVelocity(velocity.add(force));
            player.velocityModified = true; // Mark velocity as modified so it syncs to client
        }
    }

    /**
     * Handle swooping movement during the travel() phase.
     * This is called INSTEAD of vanilla travel when swooping enchantment is active.
     * Uses paper plane physics: wing angle follows velocity vector, creating emergent oscillation.
     */
    private void handleSwoopingMovement(ServerPlayerEntity player, MobilityState state, Vec3d movementInput) {
        // Increment swooping ticks
        int currentTick = state.mobility$getSwoopingTicks() + 1;
        state.mobility$setSwoopingTicks(currentTick);

        // Set fall flying flag so client renders/animates gliding properly
        // startGliding() is idempotent - safe to call every tick
        player.startGliding();

        // LOG: Entry state
        Vec3d velocityBefore = self().getVelocity();
        player.sendMessage(Text.literal(String.format("[Swooping Travel %d] START | Vel: %.3f,%.3f,%.3f | Pos: %.1f,%.1f,%.1f",
            currentTick, velocityBefore.x, velocityBefore.y, velocityBefore.z,
            self().getX(), self().getY(), self().getZ())), false);

        Vec3d velocity = velocityBefore;

        // PAPER PLANE PHYSICS: Wing angle follows velocity vector, not look direction
        double horizontalSpeed = velocity.horizontalLength();
        double gravity = this.getEffectiveGravity();
        double liftScale = MobilityConfig.SWOOPING_LIFT_MULTIPLIER;

        // Calculate pitch from velocity (for physics calculations)
        // Negative Y because in MC, positive pitch = looking down
        float pitch = (float) Math.atan2(-velocity.y, horizontalSpeed);
        double cosPitchSquared = MathHelper.square(Math.cos(pitch));

        // Wing direction follows velocity (normalized)
        // Fallback to current velocity direction if too small
        Vec3d wingDirection = velocity.lengthSquared() > 0.001
            ? velocity.normalize()
            : new Vec3d(0, -1, 0); // Default to falling straight down
        double horizontalWingMagnitude = Math.sqrt(wingDirection.x * wingDirection.x + wingDirection.z * wingDirection.z);

        // LOG: Physics inputs
        player.sendMessage(Text.literal(String.format("[Swooping Physics] VelPitch: %.1f° | HSpeed: %.3f | Gravity: %.4f",
            Math.toDegrees(pitch), horizontalSpeed, gravity)), false);

        // Base gravity adjustment with pitch-based lift
        // When velocity is horizontal (pitch ≈ 0), cos²(0) = 1 → maximum lift
        // When velocity is steep (pitch ≈ ±90°), cos²(90) = 0 → minimum lift
        velocity = velocity.add(0.0, gravity * (-1.0 + cosPitchSquared * 0.75) * liftScale, 0.0);

        // Convert downward momentum into lift along wing direction
        double liftApplied = 0.0;
        if (velocity.y < 0.0 && horizontalWingMagnitude > 0.0) {
            double lift = velocity.y * -0.1 * cosPitchSquared * liftScale;
            liftApplied = lift;
            velocity = velocity.add(
                wingDirection.x * lift / horizontalWingMagnitude,
                lift,
                wingDirection.z * lift / horizontalWingMagnitude
            );
        }

        // When velocity angles upward (pitch < 0), convert horizontal speed into altitude
        // This is the "pull up" phase after a dive
        double diveBoost = 0.0;
        if (pitch < 0.0F && horizontalWingMagnitude > 0.0) {
            double dive = horizontalSpeed * -Math.sin(pitch) * 0.04 * liftScale;
            diveBoost = dive * 3.2;
            velocity = velocity.add(
                -wingDirection.x * dive / horizontalWingMagnitude,
                dive * 3.2,
                -wingDirection.z * dive / horizontalWingMagnitude
            );
        }

        // NO velocity alignment - we don't steer toward look direction
        // This allows the emergent paper plane oscillation to occur

        // Vanilla drag
        velocity = velocity.multiply(0.99F, 0.98F, 0.99F);

        // LOG: Physics calculations
        player.sendMessage(Text.literal(String.format("[Swooping Physics] Lift: %.4f | Dive: %.4f | VelChange: %.3f,%.3f,%.3f",
            liftApplied, diveBoost,
            velocity.x - velocityBefore.x, velocity.y - velocityBefore.y, velocity.z - velocityBefore.z)), false);

        // Set the new velocity
        self().setVelocity(velocity);
        player.velocityModified = true; // Force sync to client

        // Actually move the entity (this is what vanilla travel() does)
        self().move(MovementType.SELF, self().getVelocity());

        // LOG: Final state
        Vec3d finalVelocity = self().getVelocity();
        player.sendMessage(Text.literal(String.format("[Swooping Travel %d] END | NewVel: %.3f,%.3f,%.3f | NewPos: %.1f,%.1f,%.1f",
            currentTick, finalVelocity.x, finalVelocity.y, finalVelocity.z,
            self().getX(), self().getY(), self().getZ())), false);
    }

    // ========== ELYTRA ==========

    /**
     * Handle elytra movement during the travel() phase.
     * This is called INSTEAD of vanilla travel when elytra enchantment is active.
     */
    private void handleElytraMovement(ServerPlayerEntity player, MobilityState state, Vec3d movementInput) {
        // Increment elytra ticks
        int currentTick = state.mobility$getElytraTicks() + 1;
        state.mobility$setElytraTicks(currentTick);

        // Set fall flying flag so client renders/animates gliding properly
        // startGliding() is idempotent - safe to call every tick
        player.startGliding();

        // LOG: Entry state
        Vec3d velocityBefore = self().getVelocity();
        player.sendMessage(Text.literal(String.format("[Elytra Travel %d] START | Vel: %.3f,%.3f,%.3f | Pos: %.1f,%.1f,%.1f",
            currentTick, velocityBefore.x, velocityBefore.y, velocityBefore.z,
            self().getX(), self().getY(), self().getZ())), false);

        // Apply vanilla elytra physics (from LivingEntity#calcGlidingVelocity) with configurable lift scaling
        Vec3d velocity = velocityBefore;
        Vec3d lookVector = self().getRotationVector();

        float pitch = self().getPitch() * ((float)Math.PI / 180.0F);
        double horizontalLookMagnitude = Math.sqrt(lookVector.x * lookVector.x + lookVector.z * lookVector.z);
        double horizontalSpeed = velocity.horizontalLength();
        double gravity = this.getEffectiveGravity();
        double cosPitchSquared = MathHelper.square(Math.cos(pitch));
        double liftScale = MobilityConfig.ELYTRA_LIFT_MULTIPLIER;

        // LOG: Physics inputs
        player.sendMessage(Text.literal(String.format("[Elytra Physics] Pitch: %.1f° | HSpeed: %.3f | Gravity: %.4f | LookMag: %.3f",
            self().getPitch(), horizontalSpeed, gravity, horizontalLookMagnitude)), false);

        // Base gravity adjustment with pitch-based lift (vanilla, scaled by lift multiplier)
        velocity = velocity.add(0.0, gravity * (-1.0 + cosPitchSquared * 0.75) * liftScale, 0.0);

        // Convert downward momentum into lift when pitched up (vanilla, scaled)
        double liftApplied = 0.0;
        if (velocity.y < 0.0 && horizontalLookMagnitude > 0.0) {
            double lift = velocity.y * -0.1 * cosPitchSquared * liftScale;
            liftApplied = lift;
            velocity = velocity.add(
                lookVector.x * lift / horizontalLookMagnitude,
                lift,
                lookVector.z * lift / horizontalLookMagnitude
            );
        }

        // When diving then pulling up, convert speed into altitude (vanilla, scaled)
        double diveBoost = 0.0;
        if (pitch < 0.0F && horizontalLookMagnitude > 0.0) {
            double dive = horizontalSpeed * -MathHelper.sin(pitch) * 0.04 * liftScale;
            diveBoost = dive * 3.2;
            velocity = velocity.add(
                -lookVector.x * dive / horizontalLookMagnitude,
                dive * 3.2,
                -lookVector.z * dive / horizontalLookMagnitude
            );
        }

        // Align velocity with look direction (exact vanilla behaviour, unscaled)
        if (horizontalLookMagnitude > 0.0) {
            velocity = velocity.add(
                (lookVector.x / horizontalLookMagnitude * horizontalSpeed - velocity.x) * 0.1,
                0.0,
                (lookVector.z / horizontalLookMagnitude * horizontalSpeed - velocity.z) * 0.1
            );
        }

        // Vanilla drag
        velocity = velocity.multiply(0.99F, 0.98F, 0.99F);

        // LOG: Physics calculations
        player.sendMessage(Text.literal(String.format("[Elytra Physics] Lift: %.4f | Dive: %.4f | VelChange: %.3f,%.3f,%.3f",
            liftApplied, diveBoost,
            velocity.x - velocityBefore.x, velocity.y - velocityBefore.y, velocity.z - velocityBefore.z)), false);

        // Set the new velocity
        self().setVelocity(velocity);
        player.velocityModified = true; // Force sync to client

        // Actually move the entity (this is what vanilla travel() does)
        self().move(MovementType.SELF, self().getVelocity());

        // LOG: Final state
        Vec3d finalVelocity = self().getVelocity();
        player.sendMessage(Text.literal(String.format("[Elytra Travel %d] END | NewVel: %.3f,%.3f,%.3f | NewPos: %.1f,%.1f,%.1f",
            currentTick, finalVelocity.x, finalVelocity.y, finalVelocity.z,
            self().getX(), self().getY(), self().getZ())), false);
    }
}
