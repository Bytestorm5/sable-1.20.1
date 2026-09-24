package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;

/**
 * An interface for sub-classes of {@link net.minecraft.world.level.block.entity.BlockEntity} to provide angular momentum
 * when mounted on a sub-level.
 */
public interface BlockEntitySubLevelReactionWheel {
    /**
     * Get the angular velocity of this reaction wheel, in radians per second.
     * The total angular momentum given to the sublevel is this velocity scaled by {@link dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes#INERTIA}
     * and by {@link dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes#MASS}.
     *
     * @param angularVelocity Angular velocity to be set, using {@link org.joml.Vector3d#set(double, double, double)} or similar
     */
    void sable$getAngularVelocity(Vector3d angularVelocity);

    /**
     * The default block state getter for block entities
     * @return The block state for this block entity
     * <p>
     * A default delegating to {@link BlockEntity} rather than an abstract method: on Forge 1.20.1 {@code BlockEntity}'s
     * method has its SRG name in production, so it would not implement this interface method and every implementer
     * would throw {@link AbstractMethodError}. Implementers that aren't block entities must override it.
     */
    default BlockState getBlockState() {
        return ((BlockEntity) this).getBlockState();
    }

    /**
     * Production (SRG) name of {@link #getBlockState()}. The reobfuscator may rename calls to {@code getBlockState()} made through this
     * interface to the name of the {@link BlockEntity} method it matches, in Sable or in mods built against it, so
     * the interface answers to both names.
     */
    @ApiStatus.Internal
    default BlockState m_58900_() {
        return this.getBlockState();
    }
}
