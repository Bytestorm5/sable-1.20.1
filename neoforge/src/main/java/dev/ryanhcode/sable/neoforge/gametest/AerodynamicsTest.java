package dev.ryanhcode.sable.neoforge.gametest;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.AerodynamicScaling;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Random;

import static dev.ryanhcode.sable.neoforge.gametest.SableTestHelper.*;

/**
 * Checks the quadratic aerodynamics ({@link AerodynamicScaling}) against the previous linear model of
 * {@link BlockSubLevelLiftProvider#sable$contributeLiftAndDrag}.
 */
@GameTestHolder(Sable.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AerodynamicsTest {

    /** A lift provider with fixed scalars, e.g. the default sail or Simulated's symmetric sail */
    private record TestProvider(float parallel, float directionless, float lift) implements BlockSubLevelLiftProvider {
        static final TestProvider DEFAULT = new TestProvider(0.75F, 0.06888202261f, 0.475f);
        static final TestProvider SYMMETRIC = new TestProvider(1.75F, 0.06888202261f, 0.0f);

        @Override
        public @NotNull Direction sable$getNormal(final BlockState state) {
            return Direction.UP;
        }

        @Override
        public float sable$getParallelDragScalar() {
            return this.parallel;
        }

        @Override
        public float sable$getDirectionlessDragScalar() {
            return this.directionless;
        }

        @Override
        public float sable$getLiftScalar() {
            return this.lift;
        }
    }

    /**
     * Verbatim copy of the linear model before quadratic aerodynamics (same operations in the same order), for the
     * bit-identity check.
     */
    private static void linearReference(final TestProvider provider, final BlockSubLevelLiftProvider.LiftProviderContext ctx, final ServerSubLevel subLevel,
                                        final double timeStep, final Vector3dc linearVelocity, final Vector3dc angularVelocity,
                                        final Vector3d linearImpulse, final Vector3d angularImpulse) {
        final Vector3d normal = new Vector3d(ctx.dir().x(), ctx.dir().y(), ctx.dir().z());
        final Vector3d liftPos = new Vector3d(ctx.pos().getX() + 0.5, ctx.pos().getY() + 0.5, ctx.pos().getZ() + 0.5);
        final Vector3d temp = new Vector3d();
        final Vector3d velo = new Vector3d();
        final Vector3d drag = new Vector3d();
        final Vector3d force = new Vector3d();

        final Pose3d pose = subLevel.logicalPose();
        final double pressure = DimensionPhysicsData.getAirPressure(subLevel.getLevel(), pose.transformPosition(liftPos, temp));
        pose.transformPosition(liftPos, temp).sub(pose.position());
        velo.set(linearVelocity).add(angularVelocity.cross(temp, temp));
        pose.transformNormalInverse(velo);

        if (provider.sable$getParallelDragScalar() > 0) {
            final double dragStrength = normal.dot(velo) * provider.sable$getParallelDragScalar() * pressure * timeStep;
            force.add(normal.mul(dragStrength, drag));
        }
        if (provider.sable$getDirectionlessDragScalar() > 0) {
            final double dragStrength = provider.sable$getDirectionlessDragScalar() * pressure * timeStep;
            force.add(velo.mul(dragStrength, temp));
        }
        if (provider.sable$getLiftScalar() > 0) {
            final double liftStrength = velo.sub(drag, temp).length() * provider.sable$getLiftScalar() * pressure * timeStep;
            force.add(normal.mul(liftStrength, temp));
        }

        linearImpulse.sub(force);
        liftPos.sub(subLevel.getMassTracker().getCenterOfMass(), temp);
        angularImpulse.sub(temp.cross(force));
    }

    private static ServerSubLevel spawnTestBody(final GameTestHelper helper) {
        final ServerSubLevelContainer container = SubLevelContainer.getContainer(helper.getLevel());
        if (container == null) {
            throw new IllegalStateException("Plot container not found in level");
        }
        return spawnSingleBlockSubLevel(container, absolutePosition(helper, new Vector3d(2.5, 12, 2.5)), Blocks.GLASS.defaultBlockState());
    }

    private static BlockSubLevelLiftProvider.LiftProviderContext context(final ServerSubLevel subLevel, final Vec3 normal) {
        final Vector3dc com = subLevel.getMassTracker().getCenterOfMass();
        // Offset from the centre of mass so the angular impulse is exercised too
        final BlockPos pos = BlockPos.containing(com.x() + 1.3, com.y() - 0.7, com.z() + 2.1);
        return new BlockSubLevelLiftProvider.LiftProviderContext(pos, Blocks.GLASS.defaultBlockState(), normal);
    }

    private static final List<Vec3> NORMALS = List.of(
            new Vec3(0, 1, 0), new Vec3(0, -1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, -1),
            new Vec3(1, 1, 0).normalize(), new Vec3(0.3, -0.8, 0.52).normalize());

    private static void resetOverrides() {
        AerodynamicScaling.quadraticOverride = null;
        AerodynamicScaling.referenceSpeedOverride = null;
    }

    @GameTest(template = "physicstest.gravity")
    public static void testLinearModelIsBitIdentical(final GameTestHelper helper) {
        final ServerSubLevel subLevel = spawnTestBody(helper);
        // Wait for the mass tracker to have the body's centre of mass
        helper.runAfterDelay(5, () -> testLinearModelIsBitIdenticalBody(helper, subLevel));
    }

    private static void testLinearModelIsBitIdenticalBody(final GameTestHelper helper, final ServerSubLevel subLevel) {
        final Random random = new Random(1234);
        try {
            AerodynamicScaling.quadraticOverride = false;
            int checked = 0;
            for (final TestProvider provider : List.of(TestProvider.DEFAULT, TestProvider.SYMMETRIC)) {
                for (final Vec3 normal : NORMALS) {
                    for (int i = 0; i < 200; i++) {
                        final double speed = Math.pow(10, random.nextDouble() * 4 - 1); // 0.1 .. 1000 m/s
                        final Vector3d lin = randomUnit(random).mul(speed);
                        final Vector3d ang = randomUnit(random).mul(random.nextDouble() * 3);
                        final BlockSubLevelLiftProvider.LiftProviderContext ctx = context(subLevel, normal);

                        final Vector3d linNew = new Vector3d(), angNew = new Vector3d(), linOld = new Vector3d(), angOld = new Vector3d();
                        provider.sable$contributeLiftAndDrag(ctx, subLevel, null, 1 / 40.0, lin, ang, linNew, angNew, null);
                        linearReference(provider, ctx, subLevel, 1 / 40.0, lin, ang, linOld, angOld);

                        if (!bitEquals(linNew, linOld) || !bitEquals(angNew, angOld)) {
                            helper.fail("Linear model changed: v=" + lin + " n=" + normal + " new=" + linNew + "/" + angNew + " old=" + linOld + "/" + angOld);
                            return;
                        }
                        checked++;
                    }
                }
            }
            Sable.LOGGER.info("[AerodynamicsTest] linear model bit-identical over {} samples", checked);
            helper.succeed();
        } finally {
            resetOverrides();
        }
    }

    @GameTest(template = "physicstest.gravity")
    public static void testQuadraticScalesByAirspeed(final GameTestHelper helper) {
        final ServerSubLevel subLevel = spawnTestBody(helper);
        // Wait for the mass tracker to have the body's centre of mass
        helper.runAfterDelay(5, () -> testQuadraticScalesByAirspeedBody(helper, subLevel));
    }

    private static void testQuadraticScalesByAirspeedBody(final GameTestHelper helper, final ServerSubLevel subLevel) {
        final Random random = new Random(5678);
        try {
            double worstRelativeError = 0;
            int checked = 0;
            for (final double referenceSpeed : new double[]{1.0, 2.5, 40.0}) {
                for (final Vec3 normal : NORMALS) {
                    for (final double speed : new double[]{0.05, 0.5, 1.0, 3.0, 13.3, 60.0, 250.0}) {
                        // Pure translation so the local airspeed is |v| everywhere on the body
                        final Vector3d lin = randomUnit(random).mul(speed);
                        final Vector3d ang = new Vector3d();
                        final BlockSubLevelLiftProvider.LiftProviderContext ctx = context(subLevel, normal);

                        final Vector3d linLin = new Vector3d(), angLin = new Vector3d(), linQuad = new Vector3d(), angQuad = new Vector3d();
                        AerodynamicScaling.quadraticOverride = false;
                        TestProvider.DEFAULT.sable$contributeLiftAndDrag(ctx, subLevel, null, 1 / 40.0, lin, ang, linLin, angLin, null);
                        AerodynamicScaling.quadraticOverride = true;
                        AerodynamicScaling.referenceSpeedOverride = referenceSpeed;
                        TestProvider.DEFAULT.sable$contributeLiftAndDrag(ctx, subLevel, null, 1 / 40.0, lin, ang, linQuad, angQuad, null);

                        final double s = speed / referenceSpeed;
                        final double error = Math.max(linQuad.distance(linLin.mul(s, new Vector3d())) / Math.max(linQuad.length(), 1e-300),
                                angQuad.distance(angLin.mul(s, new Vector3d())) / Math.max(angQuad.length(), 1e-300));
                        worstRelativeError = Math.max(worstRelativeError, error);
                        if (error > 1e-12) {
                            helper.fail("F_new != (|v|/V_ref)·F_lin: v=" + lin + " n=" + normal + " V_ref=" + referenceSpeed + " rel. error " + error);
                            return;
                        }
                        checked++;
                    }
                }
            }
            Sable.LOGGER.info("[AerodynamicsTest] F_new = (|v|/V_ref)·F_lin over {} samples, worst relative error {}", checked, worstRelativeError);
            helper.succeed();
        } finally {
            resetOverrides();
        }
    }

    /**
     * v·F ≥ 0 means the sails only remove energy. Scaling by s ≥ 0 must keep the sign of v·F_lin in every case, and
     * the model with no lift (Simulated's symmetric sail) must never add energy. For the default scalars the linear
     * lift term reads |v − DRAG| rather than the tangential speed, so v·F_lin itself can be slightly negative. That is
     * pre-existing behaviour, and it is logged here rather than asserted.
     */
    @GameTest(template = "physicstest.gravity")
    public static void testQuadraticNeverAddsEnergy(final GameTestHelper helper) {
        final ServerSubLevel subLevel = spawnTestBody(helper);
        // Wait for the mass tracker to have the body's centre of mass
        helper.runAfterDelay(5, () -> testQuadraticNeverAddsEnergyBody(helper, subLevel));
    }

    private static void testQuadraticNeverAddsEnergyBody(final GameTestHelper helper, final ServerSubLevel subLevel) {
        final Random random = new Random(91011);
        try {
            final Vector3d linearVelocity = new Vector3d();
            final Vector3d zero = new Vector3d();
            int samples = 0, linearNegativeDefault = 0;
            double worstLinearDefault = 0;
            for (final TestProvider provider : List.of(TestProvider.DEFAULT, TestProvider.SYMMETRIC)) {
                for (int i = 0; i < 20000; i++) {
                    final Vector3d n = randomUnit(random);
                    final double speed = Math.pow(10, random.nextDouble() * 3 - 1); // 0.1 .. 100 m/s
                    randomUnit(random).mul(speed, linearVelocity);
                    final BlockSubLevelLiftProvider.LiftProviderContext ctx = context(subLevel, new Vec3(n.x, n.y, n.z));

                    final Vector3d linLin = new Vector3d(), angLin = new Vector3d(), linQuad = new Vector3d(), angQuad = new Vector3d();
                    AerodynamicScaling.quadraticOverride = false;
                    provider.sable$contributeLiftAndDrag(ctx, subLevel, null, 1 / 40.0, linearVelocity, zero, linLin, angLin, null);
                    AerodynamicScaling.quadraticOverride = true;
                    AerodynamicScaling.referenceSpeedOverride = 1.0;
                    provider.sable$contributeLiftAndDrag(ctx, subLevel, null, 1 / 40.0, linearVelocity, zero, linQuad, angQuad, null);

                    // The impulse is minus the force in the body frame, so power = v_local · F = -v_local · impulse
                    final Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(new Vector3d(linearVelocity));
                    final double powerLinear = -localVelocity.dot(linLin);
                    final double powerQuadratic = -localVelocity.dot(linQuad);
                    final double tolerance = 1e-12 * speed * Math.max(linQuad.length(), linLin.length());

                    if (Math.signum(powerQuadratic) != Math.signum(powerLinear) && Math.abs(powerQuadratic) > tolerance) {
                        helper.fail("Quadratic scaling changed the sign of v·F: v=" + linearVelocity + " n=" + n);
                        return;
                    }
                    if (provider == TestProvider.SYMMETRIC && powerQuadratic < -tolerance) {
                        helper.fail("Symmetric sail adds energy: v=" + linearVelocity + " n=" + n + " v·F=" + powerQuadratic);
                        return;
                    }
                    if (provider == TestProvider.DEFAULT && powerLinear < -tolerance) {
                        linearNegativeDefault++;
                        worstLinearDefault = Math.min(worstLinearDefault, powerLinear / (speed * speed / 40.0));
                    }
                    samples++;
                }
            }
            Sable.LOGGER.info("[AerodynamicsTest] v·F sign preserved over {} samples; default sail (pre-existing linear lift term): {} samples with v·F < 0, worst v·F / (|v|²·dt) = {}",
                    samples, linearNegativeDefault, worstLinearDefault);
            helper.succeed();
        } finally {
            resetOverrides();
        }
    }

    @GameTest(template = "physicstest.gravity")
    public static void testSmallestEigenvalue(final GameTestHelper helper) {
        final Random random = new Random(1213);
        for (int i = 0; i < 1000; i++) {
            // R · diag(l) · Rᵀ with known eigenvalues
            final double l0 = random.nextDouble() * 10, l1 = random.nextDouble() * 10, l2 = random.nextDouble() * 10;
            final Matrix3d rotation = new Matrix3d().rotation(random.nextDouble() * 6.3, randomUnit(random));
            final Matrix3d m = new Matrix3d(rotation).mul(new Matrix3d().scaling(l0, l1, l2)).mul(new Matrix3d(rotation).transpose());
            final double expected = Math.min(l0, Math.min(l1, l2));
            final double actual = AerodynamicScaling.smallestEigenvalue(m);
            if (Math.abs(actual - expected) > 1e-9) {
                helper.fail("smallestEigenvalue: expected " + expected + " got " + actual);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * A lone sail thrown fast must slow down, not blow up: with V_ref = 1 its drag would reverse its velocity in one
     * explicit step above ~13 m/s without the stability cap.
     */
    @GameTest(template = "physicstest.gravity", timeoutTicks = 200)
    public static void testLoneSailThrownFastStaysStable(final GameTestHelper helper) {
        final Block sail = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("create:white_sail"));
        if (!(sail instanceof BlockSubLevelLiftProvider)) {
            Sable.LOGGER.warn("[AerodynamicsTest] create:white_sail is not a lift provider here, skipping");
            helper.succeed();
            return;
        }

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(helper.getLevel());
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        final ServerSubLevel subLevel = spawnSingleBlockSubLevel(container, absolutePosition(helper, new Vector3d(2.5, 30, 2.5)), sail.defaultBlockState());
        final RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
        final double[] initialSpeed = new double[1];
        final double[] maxSpeed = new double[1];

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final double speed = 60.0;
                    handle.applyLinearImpulse(absoluteDirection(helper, new Vector3d(speed, 0, speed)).normalize(speed * subLevel.getMassTracker().getMass()));
                })
                .thenExecuteAfter(1, () -> {
                    initialSpeed[0] = handle.getLinearVelocity(new Vector3d()).length();
                    maxSpeed[0] = initialSpeed[0];
                })
                .thenExecuteFor(40, () -> {
                    final Vector3d velocity = handle.getLinearVelocity(new Vector3d());
                    final double speed = velocity.length();
                    if (!Double.isFinite(speed) || speed > maxSpeed[0] * 1.05 + 5) {
                        helper.fail("Lone sail diverged: speed " + speed + " (started at " + initialSpeed[0] + ")");
                    }
                    maxSpeed[0] = Math.max(maxSpeed[0], speed);
                })
                .thenExecute(() -> {
                    final double speed = handle.getLinearVelocity(new Vector3d()).length();
                    Sable.LOGGER.info("[AerodynamicsTest] lone sail: {} m/s after the throw -> {} m/s two seconds later", initialSpeed[0], speed);
                    if (!(speed < initialSpeed[0])) {
                        helper.fail("Lone sail did not slow down: " + initialSpeed[0] + " -> " + speed);
                    }
                })
                .thenExecute(() -> removeSubLevel(container, subLevel))
                .thenSucceed();
    }

    private static Vector3d randomUnit(final Random random) {
        final Vector3d v = new Vector3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        return v.lengthSquared() < 1e-12 ? new Vector3d(0, 1, 0) : v.normalize();
    }

    private static boolean bitEquals(final Vector3dc a, final Vector3dc b) {
        return Double.doubleToRawLongBits(a.x()) == Double.doubleToRawLongBits(b.x())
                && Double.doubleToRawLongBits(a.y()) == Double.doubleToRawLongBits(b.y())
                && Double.doubleToRawLongBits(a.z()) == Double.doubleToRawLongBits(b.z());
    }
}
