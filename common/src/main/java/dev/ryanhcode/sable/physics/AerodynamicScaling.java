package dev.ryanhcode.sable.physics;

import dev.ryanhcode.sable.SableServerConfig;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;

/**
 * Airspeed scaling of sail lift and drag ({@link BlockSubLevelLiftProvider#sable$contributeLiftAndDrag}).
 * <p>
 * The linear model produces {@code F_lin = p·[k1·n(n·v) + k3·v + k2·n·|v − n(n·v)|]}. With
 * {@link SableServerConfig#QUADRATIC_AERODYNAMICS} every term is multiplied by the same factor
 * {@code s = |v| / V_ref} ({@link SableServerConfig#AERODYNAMIC_REFERENCE_SPEED}), so forces scale with airspeed squared
 * like real lift and drag. A single non-negative factor keeps {@code v·F ≥ 0} (sails never add energy, see
 * {@link BlockSubLevelLiftProvider#sable$getDirectionlessDragScalar()}) and each block's lift/drag ratio unchanged.
 * <p>
 * <b>Stability cap.</b> Forces are applied as explicit impulses, and a quadratic model gets stiff at speed: a lone
 * 0.25 kg sail at 40 Hz would reverse its own velocity in one step above ~13 m/s. So {@code s} is also capped per
 * sub-level, once per physics substep, so that the aerodynamic impulse can remove at most half of the body's momentum
 * in one step:
 * <pre>
 *   K_i       = (k1 + k3 + k2)_i · p_i                         (upper bound of |F_lin,i| / |v_i|)
 *   s_lin_cap = 0.5 · m     / (dt · Σ K_i)
 *   s_ang_cap = 0.5 · I_min / (dt · Σ K_i · |r_i|²)             (r_i = offset from the centre of mass,
 *                                                              I_min = smallest principal moment of inertia)
 *   s_i       = min(|v_i| / V_ref, s_lin_cap, s_ang_cap)
 * </pre>
 * Contraption lift providers are included in the sums. Normal craft never reach the cap. Only very light, sail-heavy,
 * fast bodies do, and for them the forces saturate instead of diverging.
 */
@ApiStatus.Internal
public final class AerodynamicScaling {

    /** Stability cap for the sub-level whose lift providers are currently being evaluated. */
    private static double stabilityCap = Double.POSITIVE_INFINITY;

    @VisibleForTesting
    @Nullable
    public static Boolean quadraticOverride = null;
    @VisibleForTesting
    @Nullable
    public static Double referenceSpeedOverride = null;

    private AerodynamicScaling() {
    }

    public static boolean isQuadratic() {
        return quadraticOverride != null ? quadraticOverride : SableServerConfig.QUADRATIC_AERODYNAMICS.get();
    }

    public static double referenceSpeed() {
        return referenceSpeedOverride != null ? referenceSpeedOverride : SableServerConfig.AERODYNAMIC_REFERENCE_SPEED.get();
    }

    /**
     * @param localVelocity the block-local airspeed vector
     * @return the factor to multiply the linear lift and drag strengths by; exactly {@code 1.0} with the linear model
     */
    public static double scale(final Vector3dc localVelocity) {
        if (!isQuadratic()) {
            return 1.0;
        }
        return Math.min(localVelocity.length() / referenceSpeed(), stabilityCap);
    }

    /**
     * Computes the stability cap for a sub-level before its lift providers are evaluated in a physics substep.
     * Must be paired with {@link #end()}.
     */
    public static void begin(final ServerSubLevel subLevel, final Collection<BlockSubLevelLiftProvider.LiftProviderContext> liftProviders,
                             final Collection<KinematicContraption> contraptions, final double partialPhysicsTick, final double timeStep) {
        stabilityCap = Double.POSITIVE_INFINITY;
        if (!isQuadratic()) {
            return;
        }

        final MassData massData = subLevel.getMassTracker();
        final Vector3dc centerOfMass = massData.getCenterOfMass();
        if (massData.isInvalid() || centerOfMass == null || timeStep <= 0) {
            return;
        }

        final double[] sums = new double[2]; // Σ K_i, Σ K_i·|r_i|²
        final Vector3d pos = new Vector3d();
        final Vector3d worldPos = new Vector3d();
        for (final BlockSubLevelLiftProvider.LiftProviderContext ctx : liftProviders) {
            accumulate(subLevel, ctx, null, centerOfMass, pos, worldPos, sums);
        }
        if (!contraptions.isEmpty()) {
            final Pose3d localPose = new Pose3d();
            for (final KinematicContraption contraption : contraptions) {
                contraption.sable$getLocalPose(localPose, partialPhysicsTick);
                for (final BlockSubLevelLiftProvider.LiftProviderContext ctx : contraption.sable$liftProviders().values()) {
                    accumulate(subLevel, ctx, localPose, centerOfMass, pos, worldPos, sums);
                }
            }
        }

        if (sums[0] > 0) {
            stabilityCap = Math.min(stabilityCap, 0.5 * massData.getMass() / (timeStep * sums[0]));
        }
        final double minInertia = smallestEigenvalue(massData.getInertiaTensor());
        // A body with a zero principal moment has no rotational inertia to protect about that axis; don't let it
        // zero every force
        if (sums[1] > 0 && minInertia > 0) {
            stabilityCap = Math.min(stabilityCap, 0.5 * minInertia / (timeStep * sums[1]));
        }
    }

    public static void end() {
        stabilityCap = Double.POSITIVE_INFINITY;
    }

    private static void accumulate(final ServerSubLevel subLevel, final BlockSubLevelLiftProvider.LiftProviderContext ctx, @Nullable final Pose3d localPose,
                                   final Vector3dc centerOfMass, final Vector3d pos, final Vector3d worldPos, final double[] sums) {
        final BlockSubLevelLiftProvider provider = (BlockSubLevelLiftProvider) ctx.state().getBlock();
        final double k = Math.max(0, provider.sable$getParallelDragScalar())
                + Math.max(0, provider.sable$getDirectionlessDragScalar())
                + Math.max(0, provider.sable$getLiftScalar());
        if (k <= 0) {
            return;
        }

        pos.set(ctx.pos().getX() + 0.5, ctx.pos().getY() + 0.5, ctx.pos().getZ() + 0.5);
        if (localPose != null) {
            localPose.transformPosition(pos);
        }
        final double pressure = DimensionPhysicsData.getAirPressure(subLevel.getLevel(), subLevel.logicalPose().transformPosition(pos, worldPos));
        final double kp = k * pressure;

        sums[0] += kp;
        sums[1] += kp * pos.distanceSquared(centerOfMass);
    }

    /**
     * @return the smallest eigenvalue of a symmetric 3x3 matrix (closed form)
     */
    @VisibleForTesting
    public static double smallestEigenvalue(final Matrix3dc m) {
        final double a = m.m00(), b = m.m11(), c = m.m22();
        final double d = 0.5 * (m.m01() + m.m10()), e = 0.5 * (m.m12() + m.m21()), f = 0.5 * (m.m02() + m.m20());
        final double p1 = d * d + e * e + f * f;
        if (p1 == 0) {
            return Math.min(a, Math.min(b, c));
        }

        final double q = (a + b + c) / 3;
        final double p2 = (a - q) * (a - q) + (b - q) * (b - q) + (c - q) * (c - q) + 2 * p1;
        final double p = Math.sqrt(p2 / 6);
        // B = (A - qI) / p, r = det(B) / 2
        final double ba = (a - q) / p, bb = (b - q) / p, bc = (c - q) / p, bd = d / p, be = e / p, bf = f / p;
        final double r = 0.5 * (ba * (bb * bc - be * be) - bd * (bd * bc - be * bf) + bf * (bd * be - bb * bf));
        final double phi = r <= -1 ? Math.PI / 3 : r >= 1 ? 0 : Math.acos(r) / 3;
        // Eigenvalues are q + 2p·cos(phi + 2πk/3); k = 1 gives the smallest
        return q + 2 * p * Math.cos(phi + 2 * Math.PI / 3);
    }
}
