package ninja.trek.mobility.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.state.MobilityState;
import ninja.trek.mobility.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Main mixin for handling continuous mobility enchantment effects.
 * Air jump activation is now handled in ServerPlayNetworkHandlerMixin.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    private LivingEntity self() {
        return (LivingEntity)(Object)this;
    }

    /**
     * Inject into tick to handle continuous effects (swooping hunger, elytra flight, etc.)
     * Air jump activation is handled in ServerPlayNetworkHandlerMixin.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }

        MobilityState state = (MobilityState) player;

        // Decrement cooldown
        if (state.mobility$getCooldown() > 0) {
            state.mobility$setCooldown(state.mobility$getCooldown() - 1);
        }

        // Reset states when player lands
        if (self().isOnGround()) {
            state.mobility$resetStates();
        }

        // Handle swooping continuous effects
        if (state.mobility$isSwooping()) {
            tickSwooping(player, state);
        }

        // Handle elytra enchantment continuous effects
        if (state.mobility$isUsingElytraEnchantment()) {
            tickElytra(player, state);
        }

        // Handle wall jumping air control
        if (state.mobility$isWallJumping()) {
            tickWallJumping(player, state);
        }
    }

    // ========== SWOOPING ==========

    private void tickSwooping(ServerPlayerEntity player, MobilityState state) {
        // Consume hunger per second
        if (player.age % 20 == 0) { // Every second
            if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.SWOOPING_HUNGER_PER_SECOND)) {
                // Out of hunger, stop swooping
                state.mobility$setSwooping(false);
                return;
            }
        }

        // Apply gliding physics - maintain current velocity with slight drag
        Vec3d velocity = self().getVelocity();
        // Apply air resistance (similar to elytra but simpler)
        double drag = 0.99;
        velocity = velocity.multiply(drag);

        // Apply gravity (less than normal)
        velocity = velocity.add(0, -0.02, 0);

        // Allow player to influence direction slightly through movement input
        // This is handled by the player's movement already, we just apply a small force
        // (The actual input handling is done in the player movement code)

        self().setVelocity(velocity);
        player.velocityModified = true; // Mark velocity as modified so it syncs to client
    }

    // ========== ELYTRA ==========

    private void tickElytra(ServerPlayerEntity player, MobilityState state) {
        // Increment elytra ticks
        state.mobility$setElytraTicks(state.mobility$getElytraTicks() + 1);

        // Consume hunger every 15 seconds
        if (state.mobility$getElytraTicks() % MobilityConfig.ELYTRA_HUNGER_TICK_INTERVAL == 0) {
            if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.ELYTRA_HUNGER_PER_15S)) {
                // Out of hunger, stop elytra
                state.mobility$setUsingElytraEnchantment(false);
                return;
            }
        }

        // Apply elytra physics with reduced lift
        Vec3d velocity = self().getVelocity();
        Vec3d lookDirection = self().getRotationVector();

        // Simplified elytra physics
        double speed = velocity.length();
        velocity = velocity.add(lookDirection.multiply(0.1 * MobilityConfig.ELYTRA_LIFT_MULTIPLIER));
        velocity = velocity.multiply(0.99); // Air resistance

        // Apply gravity
        velocity = velocity.add(0, -0.08, 0);

        self().setVelocity(velocity);
        player.velocityModified = true; // Mark velocity as modified so it syncs to client
    }

    // ========== WALL JUMP ==========

    private void tickWallJumping(ServerPlayerEntity player, MobilityState state) {
        // Wall jumping mode provides different air control
        // This is handled through the movement input system
        // We just apply speed limits here

        Vec3d velocity = self().getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Apply speed limit if needed
        if (horizontalSpeed > MobilityConfig.WALL_JUMP_SPEED_LIMIT) {
            double scale = MobilityConfig.WALL_JUMP_SPEED_LIMIT / horizontalSpeed;
            velocity = new Vec3d(velocity.x * scale, velocity.y, velocity.z * scale);
            self().setVelocity(velocity);
            player.velocityModified = true; // Mark velocity as modified so it syncs to client
        }
    }
}
