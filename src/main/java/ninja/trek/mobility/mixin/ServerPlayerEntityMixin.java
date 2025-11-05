package ninja.trek.mobility.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import ninja.trek.mobility.state.MobilityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add mobility state tracking to ServerPlayerEntity.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements MobilityState {

    @Unique
    private boolean mobility$swooping = false;

    @Unique
    private boolean mobility$wallJumping = false;

    @Unique
    private boolean mobility$usedDoubleJump = false;

    @Unique
    private boolean mobility$usingElytraEnchantment = false;

    @Unique
    private int mobility$elytraTicks = 0;

    @Unique
    private int mobility$cooldown = 0;

    @Unique
    private float mobility$swoopingHaunchAccumulator = 0.0f;

    @Unique
    private float mobility$elytraHaunchAccumulator = 0.0f;

    @Override
    public boolean mobility$isSwooping() {
        return mobility$swooping;
    }

    @Override
    public void mobility$setSwooping(boolean swooping) {
        this.mobility$swooping = swooping;
    }

    @Override
    public boolean mobility$isWallJumping() {
        return mobility$wallJumping;
    }

    @Override
    public void mobility$setWallJumping(boolean wallJumping) {
        this.mobility$wallJumping = wallJumping;
    }

    @Override
    public boolean mobility$hasUsedDoubleJump() {
        return mobility$usedDoubleJump;
    }

    @Override
    public void mobility$setUsedDoubleJump(boolean used) {
        this.mobility$usedDoubleJump = used;
    }

    @Override
    public boolean mobility$isUsingElytraEnchantment() {
        return mobility$usingElytraEnchantment;
    }

    @Override
    public void mobility$setUsingElytraEnchantment(boolean using) {
        this.mobility$usingElytraEnchantment = using;
    }

    @Override
    public int mobility$getElytraTicks() {
        return mobility$elytraTicks;
    }

    @Override
    public void mobility$setElytraTicks(int ticks) {
        this.mobility$elytraTicks = ticks;
    }

    @Override
    public int mobility$getCooldown() {
        return mobility$cooldown;
    }

    @Override
    public void mobility$setCooldown(int ticks) {
        this.mobility$cooldown = ticks;
    }

    @Override
    public void mobility$resetStates() {
        this.mobility$swooping = false;
        this.mobility$wallJumping = false;
        this.mobility$usedDoubleJump = false;
        this.mobility$usingElytraEnchantment = false;
        this.mobility$elytraTicks = 0;
        this.mobility$swoopingHaunchAccumulator = 0.0f;
        this.mobility$elytraHaunchAccumulator = 0.0f;
    }

    @Override
    public float mobility$getSwoopingHaunchAccumulator() {
        return mobility$swoopingHaunchAccumulator;
    }

    @Override
    public void mobility$setSwoopingHaunchAccumulator(float value) {
        this.mobility$swoopingHaunchAccumulator = value;
    }

    @Override
    public float mobility$getElytraHaunchAccumulator() {
        return mobility$elytraHaunchAccumulator;
    }

    @Override
    public void mobility$setElytraHaunchAccumulator(float value) {
        this.mobility$elytraHaunchAccumulator = value;
    }
}
