package dev.ryanhcode.sable.mixinhelpers.clip_overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public final class BlockGetterClipHelper {

    private BlockGetterClipHelper() {
    }

    /**
     * Vanilla's {@link BlockGetter#clip} body. Kept outside the interface mixin because Mixin 0.8.5 (Forge 1.20.1)
     * rejects non-public methods, including lambdas, in interface mixins.
     */
    public static @NotNull BlockHitResult originalClip(final BlockGetter level, final ClipContext clipContext) {
        return BlockGetter.traverseBlocks(clipContext.getFrom(), clipContext.getTo(), clipContext, (clipContextx, blockPos) -> {
            final BlockState blockState = level.getBlockState(blockPos);
            final FluidState fluidState = level.getFluidState(blockPos);
            final Vec3 vec3 = clipContextx.getFrom();
            final Vec3 vec32 = clipContextx.getTo();
            final VoxelShape voxelShape = clipContextx.getBlockShape(blockState, level, blockPos);
            final BlockHitResult blockHitResult = level.clipWithInteractionOverride(vec3, vec32, blockPos, voxelShape, blockState);
            final VoxelShape voxelShape2 = clipContextx.getFluidShape(fluidState, level, blockPos);
            final BlockHitResult blockHitResult2 = voxelShape2.clip(vec3, vec32, blockPos);
            final double d = blockHitResult == null ? Double.MAX_VALUE : clipContextx.getFrom().distanceToSqr(blockHitResult.getLocation());
            final double e = blockHitResult2 == null ? Double.MAX_VALUE : clipContextx.getFrom().distanceToSqr(blockHitResult2.getLocation());
            return d <= e ? blockHitResult : blockHitResult2;
        }, clipContextx -> {
            final Vec3 vec3 = clipContextx.getFrom().subtract(clipContextx.getTo());
            return BlockHitResult.miss(clipContextx.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(clipContextx.getTo()));
        });
    }
}
