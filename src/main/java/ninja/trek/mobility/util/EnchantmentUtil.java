package ninja.trek.mobility.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
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
        if (chestplate.isEmpty()) {
            return Optional.empty();
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
     * Try to consume hunger from the player.
     * @param player The player
     * @param amount Amount of hunger to consume (in half-drumsticks)
     * @return true if hunger was successfully consumed, false if not enough hunger
     */
    public static boolean consumeHunger(PlayerEntity player, float amount) {
        if (player.isCreative() || player.isSpectator()) {
            return true; // Creative/spectator mode always succeeds
        }

        int currentFoodLevel = player.getHungerManager().getFoodLevel();
        float currentSaturation = player.getHungerManager().getSaturationLevel();

        // Check if player has enough food
        if (currentFoodLevel <= 0 && currentSaturation <= 0) {
            return false;
        }

        // Consume hunger
        player.getHungerManager().addExhaustion(amount);
        return true;
    }
}
