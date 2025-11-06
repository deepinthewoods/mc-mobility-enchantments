package ninja.trek.mobility.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.state.MobilityState;
import ninja.trek.mobility.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Shadow;
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

    @Shadow
    protected abstract double getEffectiveGravity();

    private LivingEntity self() {
        return (LivingEntity)(Object)this;
    }

    /**
     * Inject into tick to handle state management (cooldown, hunger, landing).
     * Physics are handled in PlayerEntityMixin.travel() injection.
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
            // Stop gliding animation if active (for elytra or swooping)
            if (state.mobility$isUsingElytraEnchantment() || state.mobility$isSwooping()) {
                player.stopGliding();
            }
            state.mobility$resetStates();
        }

        // Handle swooping continuous effects (state management only, physics in travel)
        if (state.mobility$isSwooping()) {
            tickSwooping(player, state);
        }

        // Handle elytra hunger consumption
        if (state.mobility$isUsingElytraEnchantment()) {
            tickElytraHunger(player, state);
        }

        // Handle wall jumping air control
        if (state.mobility$isWallJumping()) {
            tickWallJumping(player, state);
        }
    }

    // ========== SWOOPING ==========

    private void tickSwooping(ServerPlayerEntity player, MobilityState state) {
        // Consume hunger per second
        if (player.age % 20 == 0) { // Every second (20 ticks)
            if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.SWOOPING_HUNGER_PER_SECOND)) {
                // Out of hunger, stop swooping
                player.stopGliding();
                state.mobility$setSwooping(false);
                return;
            }
        }
        // Physics are handled in PlayerEntityMixin.travel() injection
    }

    // ========== ELYTRA ==========

    /**
     * Handle hunger consumption for elytra enchantment (called from tick).
     */
    private void tickElytraHunger(ServerPlayerEntity player, MobilityState state) {
        // Consume hunger every 15 seconds
        if (state.mobility$getElytraTicks() % MobilityConfig.ELYTRA_HUNGER_TICK_INTERVAL == 0) {
            if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.ELYTRA_HUNGER_PER_15S)) {
                // Out of hunger, stop elytra
                player.sendMessage(Text.literal("[Elytra] OUT OF HUNGER - STOPPING"), false);
                player.stopGliding();
                state.mobility$setUsingElytraEnchantment(false);
            }
        }
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
