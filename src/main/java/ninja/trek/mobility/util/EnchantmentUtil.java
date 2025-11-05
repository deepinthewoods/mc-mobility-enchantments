package ninja.trek.mobility.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ninja.trek.mobility.component.ModDataComponents;
import ninja.trek.mobility.config.MobilityConfig;
import ninja.trek.mobility.enchantment.ModEnchantments;

import java.util.Optional;

/**
 * Utility methods for checking and managing mobility enchantments.
 */
public class EnchantmentUtil {

    /**
     * Get the enchantment on the player's chestplate, if any.
     * Only returns mobility enchantments.
     */
    public static Optional<RegistryKey<Enchantment>> getMobilityEnchantment(PlayerEntity player) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);

        // Debug: Check if wearing chestplate
        if (chestplate.isEmpty()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.literal("[Enchantment Debug] No chestplate equipped"), true);
            }
            return Optional.empty();
        }

        // Debug: Show what item they're wearing
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("[Enchantment Debug] Chestplate: " + chestplate.getItem().getName().getString()), true);

            // List all enchantments on the chestplate
            var enchantments = EnchantmentHelper.getEnchantments(chestplate);
            if (enchantments.isEmpty()) {
                serverPlayer.sendMessage(Text.literal("[Enchantment Debug] No enchantments on chestplate"), true);
            } else {
                serverPlayer.sendMessage(Text.literal("[Enchantment Debug] Enchantments found: " + enchantments.getEnchantments().size()), true);
            }
        }

        // Check each mobility enchantment
        if (hasEnchantment(chestplate, ModEnchantments.SWOOPING)) {
            return Optional.of(ModEnchantments.SWOOPING);
        }
        if (hasEnchantment(chestplate, ModEnchantments.DASH)) {
            return Optional.of(ModEnchantments.DASH);
        }
        if (hasEnchantment(chestplate, ModEnchantments.DOUBLE_JUMP)) {
            return Optional.of(ModEnchantments.DOUBLE_JUMP);
        }
        if (hasEnchantment(chestplate, ModEnchantments.ELYTRA)) {
            return Optional.of(ModEnchantments.ELYTRA);
        }
        if (hasEnchantment(chestplate, ModEnchantments.WALL_JUMP)) {
            return Optional.of(ModEnchantments.WALL_JUMP);
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("[Enchantment Debug] No mobility enchantments found"), true);
        }

        return Optional.empty();
    }

    /**
     * Check if an item has a specific enchantment.
     */
    public static boolean hasEnchantment(ItemStack stack, RegistryKey<Enchantment> enchantmentKey) {
        return EnchantmentHelper.getEnchantments(stack).getEnchantments().stream()
            .anyMatch(entry -> entry.matchesKey(enchantmentKey));
    }

    /**
     * Try to consume food directly from the player's food bar.
     * Subtracts the cost directly from the food level (the visible drumsticks).
     * @param player The player
     * @param amount Amount of food to consume (in half-drumsticks, so 2 = 1 full drumstick)
     * @return true if player had enough food, false if not enough
     */
    public static boolean consumeHunger(PlayerEntity player, float amount) {
        if (player.isCreative() || player.isSpectator()) {
            return true; // Creative/spectator mode always succeeds
        }

        int currentFoodLevel = player.getHungerManager().getFoodLevel();
        int cost = (int) Math.ceil(amount);

        // Check if player has enough food in their food bar (the visible drumsticks)
        if (currentFoodLevel < cost) {
            return false;
        }

        // Directly subtract from food level
        player.getHungerManager().setFoodLevel(currentFoodLevel - cost);
        return true;
    }

    /**
     * Get the number of haunches available on the player's chestplate.
     * If the chestplate doesn't have haunches data yet, initializes it.
     * @param player The player
     * @return The number of haunches, or 0 if no enchanted chestplate
     */
    public static int getHaunches(PlayerEntity player) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);

        if (chestplate.isEmpty()) {
            return 0;
        }

        // Check if this chestplate has a mobility enchantment
        if (getMobilityEnchantment(player).isEmpty()) {
            return 0;
        }

        // Get haunches from data component, or initialize with default value
        Integer haunches = chestplate.get(ModDataComponents.HAUNCHES);
        if (haunches == null) {
            // Initialize haunches on first access
            setHaunches(player, MobilityConfig.INITIAL_HAUNCHES);
            return MobilityConfig.INITIAL_HAUNCHES;
        }

        return haunches;
    }

    /**
     * Set the number of haunches on the player's chestplate.
     * @param player The player
     * @param amount The number of haunches to set
     */
    public static void setHaunches(PlayerEntity player, int amount) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);

        if (chestplate.isEmpty()) {
            return;
        }

        // Clamp to valid range
        amount = Math.max(0, Math.min(amount, MobilityConfig.MAX_HAUNCHES));

        chestplate.set(ModDataComponents.HAUNCHES, amount);
    }

    /**
     * Try to consume haunches from the player's chestplate.
     * @param player The player
     * @param amount Amount of haunches to consume (can be fractional, will round down)
     * @return true if haunches were successfully consumed, false if not enough haunches
     */
    public static boolean consumeHaunches(PlayerEntity player, float amount) {
        if (player.isCreative() || player.isSpectator()) {
            return true; // Creative/spectator mode always succeeds
        }

        int currentHaunches = getHaunches(player);
        int requiredHaunches = (int) Math.ceil(amount);

        if (currentHaunches < requiredHaunches) {
            return false;
        }

        setHaunches(player, currentHaunches - requiredHaunches);
        return true;
    }

    /**
     * Try to consume fractional haunches from the player's chestplate.
     * This uses a state object to track partial haunch consumption over time.
     * @param player The player
     * @param amount Amount of haunches to consume (fractional)
     * @param accumulated Current accumulated fractional haunches
     * @return new accumulated value after consumption
     */
    public static float consumeHaunchesGradual(PlayerEntity player, float amount, float accumulated) {
        if (player.isCreative() || player.isSpectator()) {
            return accumulated; // Creative/spectator mode doesn't consume
        }

        accumulated += amount;

        // Consume whole haunches
        int toConsume = (int) accumulated;
        if (toConsume > 0) {
            int currentHaunches = getHaunches(player);
            int actualConsumed = Math.min(toConsume, currentHaunches);
            setHaunches(player, currentHaunches - actualConsumed);
            accumulated -= actualConsumed;
        }

        return accumulated;
    }
}
