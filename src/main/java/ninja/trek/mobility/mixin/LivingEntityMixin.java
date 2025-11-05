package ninja.trek.mobility.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.enchantment.ModEnchantments;
import ninja.trek.mobility.state.MobilityState;
import ninja.trek.mobility.util.EnchantmentUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Main mixin for handling mobility enchantment mechanics.
 * Intercepts player movement and jump behavior.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected boolean jumping;

    @Unique
    private boolean lastJumpingState = false;

    private LivingEntity self() {
        return (LivingEntity)(Object)this;
    }

    /**
     * Send a debug message to the player's action bar.
     */
    private void debugMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal("[Mobility Debug] " + message), true);
    }

    /**
     * Detect when the player presses jump while in the air (like elytra activation).
     * We check the jumping flag in the tick method since the jump() method is only
     * called when on ground.
     */
    private void checkAirJumpActivation(ServerPlayerEntity player) {
        MobilityState state = (MobilityState) player;

        // Debug: Always show the state when jump key is pressed
        if (jumping) {
            debugMessage(player, String.format("Jump key pressed | lastState=%b | onGround=%b",
                lastJumpingState, self().isOnGround()));
        }

        // Detect rising edge of jump press (wasn't jumping before, is jumping now)
        if (jumping && !lastJumpingState && !self().isOnGround()) {
            // Now we know we're in the air and jump was just pressed
            debugMessage(player, "Air jump detected - checking enchantments...");

            // Check cooldown
            if (state.mobility$getCooldown() > 0) {
                debugMessage(player, "FAILED: Cooldown active (" + state.mobility$getCooldown() + " ticks remaining)");
                return;
            }

            // Get the mobility enchantment on the player's chestplate
            Optional<RegistryKey<Enchantment>> enchantment = EnchantmentUtil.getMobilityEnchantment(player);
            if (enchantment.isEmpty()) {
                debugMessage(player, "FAILED: No mobility enchantment on chestplate");
                return;
            }

            RegistryKey<Enchantment> ench = enchantment.get();
            String enchName = ench.getValue().getPath();
            debugMessage(player, "Attempting to activate: " + enchName);

            // Handle each enchantment type
            if (ench.equals(ModEnchantments.SWOOPING)) {
                handleSwooping(player, state);
            } else if (ench.equals(ModEnchantments.DASH)) {
                handleDash(player, state);
            } else if (ench.equals(ModEnchantments.DOUBLE_JUMP)) {
                handleDoubleJump(player, state);
            } else if (ench.equals(ModEnchantments.ELYTRA)) {
                handleElytra(player, state);
            } else if (ench.equals(ModEnchantments.WALL_JUMP)) {
                handleWallJump(player, state);
            }
        }

        // Update last jumping state
        lastJumpingState = jumping;
    }

    /**
     * Inject into tick to handle continuous effects (swooping hunger, elytra flight, etc.)
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }

        MobilityState state = (MobilityState) player;

        // Check for air jump activation (pressing jump while in the air)
        checkAirJumpActivation(player);

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

    private void handleSwooping(ServerPlayerEntity player, MobilityState state) {
        // Check hunger
        if (!EnchantmentUtil.consumeHunger(player, 0)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        // Activate swooping - use current velocity as direction
        state.mobility$setSwooping(true);
        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);

        // The player will glide with their current velocity vector
        // (No velocity change on activation, just enable the gliding state)

        debugMessage(player, "SUCCESS: Swooping activated");
    }

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
    }

    // ========== DASH ==========

    private void handleDash(ServerPlayerEntity player, MobilityState state) {
        // Check and consume hunger
        if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.DASH_HUNGER_COST)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        // Get the direction the player is looking
        Vec3d lookDirection = self().getRotationVector();

        // Set velocity in that direction
        Vec3d dashVelocity = lookDirection.multiply(MobilityConfig.DASH_VELOCITY);
        self().setVelocity(dashVelocity);

        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);

        debugMessage(player, "SUCCESS: Dash activated");
    }

    // ========== DOUBLE JUMP ==========

    private void handleDoubleJump(ServerPlayerEntity player, MobilityState state) {
        // Check if already used double jump
        if (state.mobility$hasUsedDoubleJump()) {
            debugMessage(player, "FAILED: Double jump already used");
            return;
        }

        // Check and consume hunger
        if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.DOUBLE_JUMP_HUNGER_COST)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        // Apply upward velocity
        Vec3d velocity = self().getVelocity();
        self().setVelocity(new Vec3d(velocity.x, MobilityConfig.DOUBLE_JUMP_VELOCITY, velocity.z));

        state.mobility$setUsedDoubleJump(true);
        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);

        debugMessage(player, "SUCCESS: Double jump activated");
    }

    // ========== ELYTRA ==========

    private void handleElytra(ServerPlayerEntity player, MobilityState state) {
        // Check hunger
        if (!EnchantmentUtil.consumeHunger(player, 0)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        // Activate elytra mode
        state.mobility$setUsingElytraEnchantment(true);
        state.mobility$setElytraTicks(0);
        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);

        // Start gliding - set velocity to look direction
        Vec3d lookDirection = self().getRotationVector();
        self().setVelocity(lookDirection.multiply(0.5));

        debugMessage(player, "SUCCESS: Elytra activated");
    }

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
    }

    // ========== WALL JUMP ==========

    private void handleWallJump(ServerPlayerEntity player, MobilityState state) {
        // Check if near a wall
        Vec3d wallNormal = detectWall(player);
        if (wallNormal == null) {
            debugMessage(player, "FAILED: No wall nearby");
            return;
        }

        // Check and consume hunger
        if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.WALL_JUMP_HUNGER_COST)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        // Enter wall jumping mode
        state.mobility$setWallJumping(true);

        // Calculate jump velocity
        // XZ component is wall normal, Y is upward at 45 degrees
        double horizontalMag = MobilityConfig.WALL_JUMP_VELOCITY * Math.cos(Math.PI / 4);
        double verticalMag = MobilityConfig.WALL_JUMP_VELOCITY * Math.sin(Math.PI / 4);

        Vec3d jumpVelocity = new Vec3d(
            wallNormal.x * horizontalMag,
            verticalMag,
            wallNormal.z * horizontalMag
        );

        self().setVelocity(jumpVelocity);
        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);

        debugMessage(player, "SUCCESS: Wall jump activated");
    }

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
        }
    }

    /**
     * Detect if the player is next to a wall and return the wall normal.
     * Returns null if not near a wall.
     */
    private Vec3d detectWall(ServerPlayerEntity player) {
        // Probe points at top and bottom of player on all 4 sides
        double dist = MobilityConfig.WALL_DETECTION_DISTANCE;
        BlockPos playerPos = player.getBlockPos();

        Vec3d playerCenter = new Vec3d(player.getX(), player.getY(), player.getZ());
        double playerTop = playerCenter.y + 1.5;
        double playerBottom = playerCenter.y + 0.2;

        // Check 4 cardinal directions
        Vec3d[] directions = {
            new Vec3d(1, 0, 0),  // East
            new Vec3d(-1, 0, 0), // West
            new Vec3d(0, 0, 1),  // South
            new Vec3d(0, 0, -1)  // North
        };

        Vec3d totalNormal = Vec3d.ZERO;
        int wallCount = 0;

        for (Vec3d dir : directions) {
            // Check top and bottom probe points
            Vec3d topProbe = new Vec3d(playerCenter.x + dir.x * (0.3 + dist), playerTop, playerCenter.z + dir.z * (0.3 + dist));
            Vec3d bottomProbe = new Vec3d(playerCenter.x + dir.x * (0.3 + dist), playerBottom, playerCenter.z + dir.z * (0.3 + dist));

            BlockPos topBlock = BlockPos.ofFloored(topProbe);
            BlockPos bottomBlock = BlockPos.ofFloored(bottomProbe);

            // Check if these positions have solid blocks
            if (!player.getEntityWorld().getBlockState(topBlock).isAir() || !player.getEntityWorld().getBlockState(bottomBlock).isAir()) {
                totalNormal = totalNormal.add(dir.multiply(-1)); // Normal points away from wall
                wallCount++;
            }
        }

        if (wallCount == 0) {
            return null; // No wall
        }

        // If at a corner (2 walls), the normal is 45 degrees between them
        return totalNormal.normalize();
    }
}
