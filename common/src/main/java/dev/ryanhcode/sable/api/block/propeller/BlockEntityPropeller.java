package dev.ryanhcode.sable.api.block.propeller;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3d;

import net.minecraft.util.Mth;
/**
 * Spinny spin spin, woosh woosh!
 */
public interface BlockEntityPropeller {

    /**
     * @return the direction of the propeller
     */
    Direction getBlockDirection();

    /**
     * @return airflow in units of [m/s]
     */
    double getAirflow();

    /**
     * @return thrust in [pN]
     */
    double getThrust();

    /**
     * @return if the propeller is active / thrust should be computed
     */
    boolean isActive();

    /**
     * @return the thrust scaled by -1 * airflow scaling * air pressure
     */
    default double getScaledThrust() {
        return -this.getThrust() * this.getAirflowScaling() * this.getCurrentAirPressure();
    }

    default double getCurrentAirPressure() {
        final Level level = this.getPropellerLevel();
        return DimensionPhysicsData.getAirPressure(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(this.getPropellerPos().getCenter())));
    }

    default double getAirflowScaling() {
        final double airflow = this.getAirflow();

        if (Math.abs(airflow) <= 0.001) {
            return 1.0;
        }

        final Level level = this.getPropellerLevel();
        final Vector3d pos = JOMLConversion.toJOML(this.getPropellerPos().getCenter());
        final SubLevel subLevel = Sable.HELPER.getContaining(level, this.getPropellerPos());

        if (subLevel == null) {
            return 1.0;
        }

        final Vector3d velocity = Sable.HELPER.getVelocity(level, subLevel, pos, new Vector3d());
        final Vector3d thrustDirection = subLevel.logicalPose().transformNormal(JOMLConversion.atLowerCornerOf(this.getBlockDirection().getNormal()));

        return Mth.clamp((airflow + velocity.dot(thrustDirection.x, thrustDirection.y, thrustDirection.z)) / airflow, 0, 1);
    }

    /**
     * @return the level of this propeller's block entity
     * <p>
     * Deliberately not named like the {@link BlockEntity} method: on Forge 1.20.1 a Sable interface method that shares a
     * name with a Minecraft method is either left unimplemented in production ({@link AbstractMethodError}) or clashes
     * when mods built against Sable remap it. Non-block-entity implementers must override it.
     */
    default Level getPropellerLevel() {
        return ((BlockEntity) this).getLevel();
    }

    /**
     * @return the position of this propeller's block entity
     * <p>
     * Deliberately not named like the {@link BlockEntity} method: on Forge 1.20.1 a Sable interface method that shares a
     * name with a Minecraft method is either left unimplemented in production ({@link AbstractMethodError}) or clashes
     * when mods built against Sable remap it. Non-block-entity implementers must override it.
     */
    default BlockPos getPropellerPos() {
        return ((BlockEntity) this).getBlockPos();
    }
}
