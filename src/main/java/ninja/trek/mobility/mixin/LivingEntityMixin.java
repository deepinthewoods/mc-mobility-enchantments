package ninja.trek.mobility.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.enchantment.ModEnchantments;
import ninja.trek.mobility.physics.ElytraPhysics;
import ninja.trek.mobility.state.MobilityState;
import ninja.trek.mobility.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Main mixin for handling continuous mobility enchantment effects.
 * Air jump activation is now handled in ServerPlayNetworkHandlerMixin.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private Vec3d mobility$preTickVelocity = Vec3d.ZERO;

    private LivingEntity self() {
        return (LivingEntity)(Object)this;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void mobility$capturePreTickVelocity(CallbackInfo ci) {
        this.mobility$preTickVelocity = self().getVelocity();
    }

    /**
     * Inject into tick to handle state management (cooldown, hunger, landing) and
     * run our custom Elytra physics once vanilla is done with its update.
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

        boolean hasElytraEnchant = hasElytraEnchant(player);
        boolean isGliding = player.isGliding() && hasElytraEnchant;

        if (isGliding) {
            state.mobility$setElytraGliding(true);
            state.mobility$setWallJumping(false);

            if (!player.isCreative() && !player.isSpectator()) {
                float exhaustionPerTick = (float) (MobilityConfig.ELYTRA_HUNGER_PER_15S * 4.0D / MobilityConfig.ELYTRA_HUNGER_TICK_INTERVAL);
                if (exhaustionPerTick > 0.0F) {
                    float accumulated = state.mobility$getElytraHungerRemainder() + exhaustionPerTick;
                    if (accumulated >= 0.01F) {
                        player.addExhaustion(accumulated);
                        accumulated = 0.0F;
                    }
                    state.mobility$setElytraHungerRemainder(accumulated);
                }
            } else {
                state.mobility$setElytraHungerRemainder(0.0F);
            }
        } else if (state.mobility$isElytraGliding()) {
            state.mobility$setElytraGliding(false);
            state.mobility$setElytraHungerRemainder(0.0F);
        }

        // Handle wall jumping air control
        if (state.mobility$isWallJumping()) {
            tickWallJumping(player, state);
        }

        maybeApplyElytraPhysics(player, state);
    }

    @Inject(method = "canGlideWith", at = @At("HEAD"), cancellable = true)
    private static void mobility$elytraCanGlide(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        if (slot != EquipmentSlot.CHEST) {
            return;
        }

        if (!EnchantmentUtil.hasEnchantment(stack, ModEnchantments.ELYTRA)) {
            return;
        }

        if (stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 1) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(true);
    }

    private boolean hasElytraEnchant(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        return EnchantmentUtil.hasEnchantment(chest, ModEnchantments.ELYTRA);
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

    private void maybeApplyElytraPhysics(ServerPlayerEntity player, MobilityState state) {
        if (!state.mobility$isElytraGliding()) {
            return;
        }

        if (!hasElytraEnchant(player)) {
            return;
        }

        Vec3d oldVelocity = mobility$preTickVelocity;
        Vec3d newVelocity = ElytraPhysics.computeGlideVelocity(player, oldVelocity, ((LivingEntityAccessor) this).invokeGetEffectiveGravity());
        player.setVelocity(newVelocity);
        player.velocityModified = true;
    }
}
