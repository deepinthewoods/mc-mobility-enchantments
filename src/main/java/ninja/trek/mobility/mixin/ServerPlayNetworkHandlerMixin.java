package ninja.trek.mobility.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayNetworkHandler;
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
 * Mixin to detect jump input packets for air jump activation.
 * This is the same technique vanilla uses for elytra deployment.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Unique
    private boolean lastJumpInput = false;

    /**
     * Send a debug message to the player's action bar.
     */
    @Unique
    private void debugMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal("[Mobility Debug] " + message), !true);
    }

    /**
     * Intercept player input packets to detect jump presses while airborne.
     * This is how vanilla elytra activation works - it checks the jump input
     * from the packet, not a synchronized field.
     */
    @Inject(method = "onPlayerInput", at = @At("HEAD"))
    private void onPlayerInput(PlayerInputC2SPacket packet, CallbackInfo ci) {
        // Get current jump input from packet
        boolean currentJumpInput = packet.input().jump();

        // Reset jump state when on ground to ensure clean rising edge detection
        // This fixes the issue where packets with jump=false might not arrive between presses
        if (player.isOnGround()) {
            lastJumpInput = false;
        }

        // Debug: Show ALL input packets to understand the packet flow
        debugMessage(player, String.format("Input packet | jump=%b | lastJump=%b | onGround=%b",
            currentJumpInput, lastJumpInput, player.isOnGround()));

        // Detect rising edge: jump pressed this tick but not last tick
        if (currentJumpInput && !lastJumpInput && !player.isOnGround()) {
            // Jump was just pressed while in the air!
            handleAirJumpActivation();

        }

        // Update state for next packet
        lastJumpInput = currentJumpInput;
    }

    /**
     * Handle air jump activation - check enchantments and trigger abilities.
     */
    @Unique
    private void handleAirJumpActivation() {
        MobilityState state = (MobilityState) player;

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

        if (ench.equals(ModEnchantments.DASH)) {
            handleDash(state);
        } else if (ench.equals(ModEnchantments.DOUBLE_JUMP)) {
            handleDoubleJump(state);
        } else if (ench.equals(ModEnchantments.WALL_JUMP)) {
            handleWallJump(state);
        }

    }

    /**
     * Listen for vanilla's start-fall-flying command so we can piggyback on the
     * exact timing vanilla uses (the client sends this when the player double-taps jump).
     */
    @Inject(method = "onClientCommand", at = @At("HEAD"), cancellable = true)
    private void mobility$onClientCommand(ClientCommandC2SPacket packet, CallbackInfo ci) {
        if (packet.getMode() != ClientCommandC2SPacket.Mode.START_FALL_FLYING) {
            return;
        }

        MobilityState state = (MobilityState) player;
        boolean handled = handleElytra(state);

        if (handled) {
            ci.cancel(); // Prevent vanilla from running its own logic with a non-elytra chestplate
        }
    }

    // ========== ELYTRA ==========

    @Unique
    private boolean handleElytra(MobilityState state) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!EnchantmentUtil.hasEnchantment(chestplate, ModEnchantments.ELYTRA)) {
            debugMessage(player, "FAILED: Elytra enchantment missing");
            return false;
        }

        if (player.isGliding()) {
            debugMessage(player, "FAILED: Already gliding");
            return false;
        }

        if (player.isOnGround()) {
            debugMessage(player, "FAILED: Must be airborne to start gliding");
            return false;
        }

        if (player.hasVehicle()) {
            debugMessage(player, "FAILED: Cannot glide while riding");
            return false;
        }

        if (player.isTouchingWater()) {
            debugMessage(player, "FAILED: Cannot glide while touching water");
            return false;
        }

        if (player.hasStatusEffect(StatusEffects.LEVITATION)) {
            debugMessage(player, "FAILED: Levitation prevents gliding");
            return false;
        }

        if (chestplate.isDamageable() && chestplate.getDamage() >= chestplate.getMaxDamage() - 1) {
            debugMessage(player, "FAILED: Chestplate would break on glide start");
            return false;
        }

        player.startGliding();
        state.mobility$setElytraGliding(true);
        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);
        state.mobility$setWallJumping(false);
        debugMessage(player, "SUCCESS: Elytra glide activated");
        return true;
    }

    // ========== DASH ==========

    @Unique
    private void handleDash(MobilityState state) {
        if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.DASH_HUNGER_COST)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        Vec3d lookDirection = player.getRotationVector();
        Vec3d dashVelocity = lookDirection.multiply(MobilityConfig.DASH_VELOCITY);
        player.setVelocity(dashVelocity);
        player.velocityModified = true; // Mark velocity as modified so it syncs to client

        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);
        debugMessage(player, "SUCCESS: Dash activated");
    }

    // ========== DOUBLE JUMP ==========

    @Unique
    private void handleDoubleJump(MobilityState state) {
        // Unlimited double jumps! Just subtracts from food bar
        if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.DOUBLE_JUMP_HUNGER_COST)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        Vec3d velocity = player.getVelocity();
        player.setVelocity(new Vec3d(velocity.x, MobilityConfig.DOUBLE_JUMP_VELOCITY, velocity.z));
        player.velocityModified = true; // Mark velocity as modified so it syncs to client

        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);
        debugMessage(player, "SUCCESS: Double jump activated");
    }

    // Additional mobility abilities (e.g., future swooping support) handled below

    // ========== WALL JUMP ==========

    @Unique
    private void handleWallJump(MobilityState state) {
        Vec3d wallNormal = detectWall();
        if (wallNormal == null) {
            debugMessage(player, "FAILED: No wall nearby");
            return;
        }

        if (!EnchantmentUtil.consumeHunger(player, MobilityConfig.WALL_JUMP_HUNGER_COST)) {
            debugMessage(player, "FAILED: Not enough hunger");
            return;
        }

        state.mobility$setWallJumping(true);

        double horizontalMag = MobilityConfig.WALL_JUMP_VELOCITY * Math.cos(Math.PI / 4);
        double verticalMag = MobilityConfig.WALL_JUMP_VELOCITY * Math.sin(Math.PI / 4);

        Vec3d jumpVelocity = new Vec3d(
            wallNormal.x * horizontalMag,
            verticalMag,
            wallNormal.z * horizontalMag
        );

        player.setVelocity(jumpVelocity);
        player.velocityModified = true; // Mark velocity as modified so it syncs to client
        state.mobility$setCooldown(MobilityConfig.ABILITY_COOLDOWN_TICKS);
        debugMessage(player, "SUCCESS: Wall jump activated");
    }

    @Unique
    private Vec3d detectWall() {
        double dist = MobilityConfig.WALL_DETECTION_DISTANCE;
        Vec3d playerCenter = new Vec3d(player.getX(), player.getY(), player.getZ());
        double playerTop = playerCenter.y + 1.5;
        double playerBottom = playerCenter.y + 0.2;

        Vec3d[] directions = {
            new Vec3d(1, 0, 0),
            new Vec3d(-1, 0, 0),
            new Vec3d(0, 0, 1),
            new Vec3d(0, 0, -1)
        };

        Vec3d totalNormal = Vec3d.ZERO;
        int wallCount = 0;

        for (Vec3d dir : directions) {
            Vec3d topProbe = new Vec3d(playerCenter.x + dir.x * (0.3 + dist), playerTop, playerCenter.z + dir.z * (0.3 + dist));
            Vec3d bottomProbe = new Vec3d(playerCenter.x + dir.x * (0.3 + dist), playerBottom, playerCenter.z + dir.z * (0.3 + dist));

            if (!player.getEntityWorld().getBlockState(BlockPos.ofFloored(topProbe)).isAir() ||
                !player.getEntityWorld().getBlockState(BlockPos.ofFloored(bottomProbe)).isAir()) {
                totalNormal = totalNormal.add(dir.multiply(-1));
                wallCount++;
            }
        }

        if (wallCount == 0) {
            return null;
        }

        return totalNormal.normalize();
    }
}
