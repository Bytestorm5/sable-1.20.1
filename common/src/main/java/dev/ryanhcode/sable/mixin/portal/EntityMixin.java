package dev.ryanhcode.sable.mixin.portal;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow
    public abstract Pose getPose();

    @Shadow
    public abstract Level level();

    /**
     * @author RyanH
     * @reason Going through portals on sub-levels
     */
    @Overwrite
    protected Vec3 getRelativePortalPosition(final Direction.Axis axis, final BlockUtil.FoundRectangle foundRectangle) {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.level(), foundRectangle.minCorner);
        Vec3 position = this.position();

        if (subLevel != null) {
            position = subLevel.logicalPose().transformPositionInverse(position);
        }

        return PortalShape.getRelativePosition(foundRectangle, axis, position, this.getDimensions(this.getPose()));
    }

    /**
     * Uses the global position of entities in sub-levels to find the portal destination.
     * <p>
     * 1.21 does this in {@code NetherPortalBlock#getPortalDestination}; on 1.20.1 it's {@code Entity#findDimensionEntryPoint}.
     */
    @Redirect(method = "findDimensionEntryPoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;clampToBounds(DDD)Lnet/minecraft/core/BlockPos;"))
    private BlockPos sable$getPortalDestination(final WorldBorder instance,
                                                final double x,
                                                final double y,
                                                final double z,
                                                @Local(ordinal = 0) final double multiplier) {
        final Vec3 globalPos = Sable.HELPER.projectOutOfSubLevel(this.level(), this.position());

        return instance.clampToBounds(
                globalPos.x * multiplier,
                globalPos.y,
                globalPos.z * multiplier
        );
    }

}
