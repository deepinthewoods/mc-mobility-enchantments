package ninja.trek.mobility.component;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ninja.trek.mobility.MobilityEnchantments;

/**
 * Custom data components for mobility enchantments.
 */
public class ModDataComponents {

    /**
     * Stores the number of haunches (charges) available on an enchanted item.
     * Haunches are consumed when using mobility enchantments.
     */
    public static final ComponentType<Integer> HAUNCHES = register(
        "haunches",
        ComponentType.<Integer>builder()
            .codec(Codec.INT)
            .build()
    );

    private static <T> ComponentType<T> register(String name, ComponentType<T> component) {
        return Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MobilityEnchantments.MOD_ID, name),
            component
        );
    }

    /**
     * Called during mod initialization to register all data components.
     */
    public static void initialize() {
        MobilityEnchantments.LOGGER.info("Registering data components");
    }
}
