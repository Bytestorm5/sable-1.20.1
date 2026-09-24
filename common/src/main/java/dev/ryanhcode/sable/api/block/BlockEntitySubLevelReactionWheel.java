package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
     * @return the block state of this reaction wheel's block entity
     * <p>
     * Deliberately not named like the {@link BlockEntity} method: on Forge 1.20.1 a Sable interface method that shares a
     * name with a Minecraft method is either left unimplemented in production ({@link AbstractMethodError}) or clashes
     * when mods built against Sable remap it. Non-block-entity implementers must override it.
     */
    default BlockState getReactionWheelState() {
        return ((BlockEntity) this).getBlockState();
    }
}
