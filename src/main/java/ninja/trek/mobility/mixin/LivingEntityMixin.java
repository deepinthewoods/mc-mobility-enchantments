package ninja.trek.mobility.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.enchantment.ModEnchantments;
import ninja.trek.mobility.state.MobilityState;
import ninja.trek.mobility.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
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

    @Inject(method = "calcGlidingVelocity", at = @At("HEAD"), cancellable = true)
    private void mobility$overrideGlidePhysics(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
        ItemStack chest = self().getEquippedStack(EquipmentSlot.CHEST);
        if (!EnchantmentUtil.hasEnchantment(chest, ModEnchantments.ELYTRA)) {
            return;
        }

        Vec3d rotation = self().getRotationVector();
        float pitchRadians = self().getPitch() * (float) (Math.PI / 180.0);
        double horizontalRotation = Math.sqrt(rotation.x * rotation.x + rotation.z * rotation.z);
        double horizontalSpeed = oldVelocity.horizontalLength();
        double gravity = ((LivingEntityAccessor) this).invokeGetEffectiveGravity();
        double cosSquared = MathHelper.square(Math.cos(pitchRadians));

        double liftMultiplier = MobilityConfig.ELYTRA_LIFT_MULTIPLIER;
        Vec3d velocity = oldVelocity.add(0.0, gravity * (-1.0 + cosSquared * 0.75 * liftMultiplier), 0.0);

        if (velocity.y < 0.0 && horizontalRotation > 0.0) {
            double adjust = velocity.y * -0.1 * cosSquared;
            velocity = velocity.add(rotation.x * adjust / horizontalRotation, adjust, rotation.z * adjust / horizontalRotation);
        }

        if (pitchRadians < 0.0F && horizontalRotation > 0.0) {
            double adjust = horizontalSpeed * -MathHelper.sin(pitchRadians) * 0.04;
            velocity = velocity.add(-rotation.x * adjust / horizontalRotation, adjust * 3.2, -rotation.z * adjust / horizontalRotation);
        }

        if (horizontalRotation > 0.0) {
            double adjustX = (rotation.x / horizontalRotation * horizontalSpeed - velocity.x) * 0.1;
            double adjustZ = (rotation.z / horizontalRotation * horizontalSpeed - velocity.z) * 0.1;
            velocity = velocity.add(adjustX, 0.0, adjustZ);
        }

        cir.setReturnValue(velocity.multiply(0.99F, 0.98F, 0.99F));
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
}
