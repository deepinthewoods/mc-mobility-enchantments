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

    private PlayerEntity self() {
        return (PlayerEntity) (Object) this;
    }

    /**
     * Inject into travel method to handle wall jumping movement adjustments.
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = self();

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        MobilityState state = (MobilityState) serverPlayer;

        //TODO

        if (state.mobility$isWallJumping() && !player.isOnGround()) {
            handleWallJumpingMovement(serverPlayer, movementInput);
        }
    }

    private void handleWallJumpingMovement(ServerPlayerEntity player, Vec3d movementInput) {
        if (movementInput.lengthSquared() > 0) {
            Vec3d velocity = player.getVelocity();

            float yaw = player.getYaw();
            double yawRad = Math.toRadians(yaw);

            double forward = movementInput.z;
            double strafe = movementInput.x;

            double moveX = -Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe;
            double moveZ = Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe;

            Vec3d force = new Vec3d(moveX, 0, moveZ).normalize().multiply(MobilityConfig.WALL_JUMP_AIR_CONTROL);
            player.setVelocity(velocity.add(force));
            player.velocityModified = true;
        }
    }
}
