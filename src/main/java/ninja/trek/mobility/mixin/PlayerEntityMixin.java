package ninja.trek.mobility.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.state.MobilityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle player movement modifications for mobility enchantments.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    /**
     * Inject into travel method to handle custom air movement for wall jumping and swooping.
     */
    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Only handle server-side
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        MobilityState state = (MobilityState) serverPlayer;

        // Handle wall jumping air control
        if (state.mobility$isWallJumping() && !player.isOnGround()) {
            handleWallJumpingMovement(serverPlayer, movementInput);
        }

        // Handle swooping air control
        if (state.mobility$isSwooping() && !player.isOnGround()) {
            handleSwoopingMovement(serverPlayer, movementInput);
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
        }
    }

    private void handleSwoopingMovement(ServerPlayerEntity player, Vec3d movementInput) {
        // Apply small horizontal force based on movement input
        // Similar to normal falling movement
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
            Vec3d force = new Vec3d(moveX, 0, moveZ).normalize().multiply(MobilityConfig.SWOOPING_AIR_CONTROL);
            player.setVelocity(velocity.add(force));
        }
    }
}
