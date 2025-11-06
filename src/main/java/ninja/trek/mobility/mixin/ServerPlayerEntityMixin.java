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
    private boolean mobility$wallJumping = false;

    @Unique
    private boolean mobility$usedDoubleJump = false;

    //TODO

    @Unique
    private int mobility$cooldown = 0;

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

    //TODO

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
        this.mobility$wallJumping = false;
        this.mobility$usedDoubleJump = false;
        //TODO
    }
}
