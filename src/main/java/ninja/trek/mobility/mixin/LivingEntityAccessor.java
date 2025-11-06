package ninja.trek.mobility.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor to expose protected methods from LivingEntity.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("getEffectiveGravity")
    double invokeGetEffectiveGravity();
}
