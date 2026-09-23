package dev.ryanhcode.sable.mixinhelpers.entity.entity_pathfinding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

/**
 * Stand-in for 1.21's {@code PathfindingContext#mobPosition}, which Sable made local to the sub-level a mob is standing on.
 * 1.20.1 has no pathfinding context, so the node evaluators read {@link Mob#blockPosition()} directly and are redirected here.
 */
public final class PathfindingMobPositionHelper {

    private PathfindingMobPositionHelper() {
    }

    /**
     * @param mob the pathfinding mob
     * @return the block position of the mob, in the plot of the sub-level it is tracking if any
     */
    public static BlockPos getMobPosition(final Mob mob) {
        final SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(mob);

        if (trackingSubLevel != null) {
            return BlockPos.containing(trackingSubLevel.logicalPose().transformPositionInverse(mob.position()));
        }

        return mob.blockPosition();
    }
}
