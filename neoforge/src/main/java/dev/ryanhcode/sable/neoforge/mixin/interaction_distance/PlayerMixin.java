package dev.ryanhcode.sable.neoforge.mixin.interaction_distance;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.extensions.IForgePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Fixes interaction distance on entity and block interactions.
 * <p>
 * 1.21 funnels these checks through {@code Player#canInteractWithBlock} / {@code canInteractWithEntity}. On Forge 1.20.1
 * the server checks go through the {@link IForgePlayer} reach methods instead, so we override those default methods
 * on {@link Player}. The vanilla bodies are kept for positions that aren't in a sub-level.
 */
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IForgePlayer {

    protected PlayerMixin(final EntityType<? extends LivingEntity> entityType, final Level level) {
        super(entityType, level);
    }

    @Override
    public boolean canReach(final BlockPos pos, final double padding) {
        final double reach = this.getBlockReach() + padding;
        return this.sable$canReachBlock(pos, reach);
    }

    @Override
    public boolean canReachRaw(final BlockPos pos, final double padding) {
        final double reach = this.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + padding;
        return this.sable$canReachBlock(pos, reach);
    }

    @Override
    public boolean isCloseEnough(final Entity entity, final double dist) {
        final AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());

        // should bottom center be assumed here?
        final SubLevel subLevel = Sable.HELPER.getContaining(this.level(), new Vec3(aabb.getCenter().x, aabb.minY, aabb.getCenter().z));

        if (subLevel != null) {
            final Vec3 eyePos = subLevel.logicalPose().transformPositionInverse(this.getEyePosition());

            if (aabb.distanceToSqr(eyePos) < dist * dist) {
                return true;
            }
        }

        return aabb.distanceToSqr(this.getEyePosition()) < dist * dist;
    }

    @Unique
    private boolean sable$canReachBlock(final BlockPos pos, final double reach) {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.level(), pos);

        if (subLevel != null) {
            final Vec3 eyePos = subLevel.logicalPose().transformPositionInverse(this.getEyePosition());

            if (eyePos.distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach) {
                return true;
            }
        }

        return this.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach;
    }

}
