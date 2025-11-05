package ninja.trek.mobility.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import ninja.trek.mobility.MobilityEnchantments;

/**
 * Registry keys for all mobility enchantments.
 * Since 1.21, enchantments are data-driven and stored as RegistryKeys.
 */
public class ModEnchantments {

    // Mobility enchantments - all mutually exclusive with each other and protection variants
    public static final RegistryKey<Enchantment> SWOOPING = of("swooping");
    public static final RegistryKey<Enchantment> DASH = of("dash");
    public static final RegistryKey<Enchantment> DOUBLE_JUMP = of("double_jump");
    public static final RegistryKey<Enchantment> ELYTRA = of("elytra");
    public static final RegistryKey<Enchantment> WALL_JUMP = of("wall_jump");

    /**
     * Helper method to create a RegistryKey for an enchantment
     */
    private static RegistryKey<Enchantment> of(String name) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT,
            Identifier.of(MobilityEnchantments.MOD_ID, name));
    }

    /**
     * Initialize the enchantments. Called during mod initialization.
     * With data-driven enchantments, this just ensures the class is loaded.
     */
    public static void initialize() {
        MobilityEnchantments.LOGGER.info("Registering mobility enchantments");
    }
}
