package dev.ryanhcode.sable.api.block.propeller;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
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
        final Level level = this.getLevel();
        return DimensionPhysicsData.getAirPressure(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(this.getBlockPos().getCenter())));
    }

    default double getAirflowScaling() {
        final double airflow = this.getAirflow();

        if (Math.abs(airflow) <= 0.001) {
            return 1.0;
        }

        final Level level = this.getLevel();
        final Vector3d pos = JOMLConversion.toJOML(this.getBlockPos().getCenter());
        final SubLevel subLevel = Sable.HELPER.getContaining(level, this.getBlockPos());

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
     * A default delegating to {@link BlockEntity} rather than an abstract method: on Forge 1.20.1 {@code BlockEntity}'s
     * method has its SRG name in production, so it would not implement this interface method and every implementer
     * would throw {@link AbstractMethodError}. Implementers that aren't block entities must override it.
     */
    default Level getLevel() {
        return ((BlockEntity) this).getLevel();
    }

    /**
     * @return the position of this propeller's block entity
     * <p>
     * A default delegating to {@link BlockEntity} rather than an abstract method: on Forge 1.20.1 {@code BlockEntity}'s
     * method has its SRG name in production, so it would not implement this interface method and every implementer
     * would throw {@link AbstractMethodError}. Implementers that aren't block entities must override it.
     */
    default BlockPos getBlockPos() {
        return ((BlockEntity) this).getBlockPos();
    }

    /**
     * Production (SRG) name of {@link #getLevel()}. The reobfuscator may rename calls to {@code getLevel()} made through this
     * interface to the name of the {@link BlockEntity} method it matches, in Sable or in mods built against it, so
     * the interface answers to both names.
     */
    @ApiStatus.Internal
    default Level m_58904_() {
        return this.getLevel();
    }

    /**
     * Production (SRG) name of {@link #getBlockPos()}. The reobfuscator may rename calls to {@code getBlockPos()} made through this
     * interface to the name of the {@link BlockEntity} method it matches, in Sable or in mods built against it, so
     * the interface answers to both names.
     */
    @ApiStatus.Internal
    default BlockPos m_58899_() {
        return this.getBlockPos();
    }
}

