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
}
