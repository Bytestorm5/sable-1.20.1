package dev.ryanhcode.sable.mixin.entity.entity_leashing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21 moved leash physics into the {@code Leashable} interface ({@code legacyElasticRangeLeashBehaviour}).
 * On 1.20.1 the same elastic pull is inlined in {@link PathfinderMob#tickLeash()}, so we replace the impulse it applies.
 */
@Mixin(PathfinderMob.class)
public abstract class PathfinderMobMixin extends Mob {

    protected PathfinderMobMixin(final EntityType<? extends Mob> entityType, final Level level) {
        super(entityType, level);
    }

    /**
     * Take into account sub-levels for the elastic leash impulse
     */
    @WrapOperation(method = "tickLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/PathfinderMob;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void sable$elasticRangeLeashBehaviour(final PathfinderMob leashedEntity,
                                                   final Vec3 originalImpulse,
                                                   final Operation<Void> original,
                                                   @Local(ordinal = 0) final Entity handlerEntity,
                                                   @Local(ordinal = 0) final float f) {
        final ActiveSableCompanion helper = Sable.HELPER;
        final Level level = handlerEntity.level();
        final Vec3 handlerPos = helper.projectOutOfSubLevel(level, handlerEntity.position());
        final Vec3 leashedPos = helper.projectOutOfSubLevel(level, leashedEntity.position());
        final double d = (handlerPos.x - leashedPos.x) / (double)f;
        final double e = (handlerPos.y - leashedPos.y) / (double)f;
        final double g = (handlerPos.z - leashedPos.z) / (double)f;

        Vec3 impulse = leashedEntity.getDeltaMovement().add(Math.copySign(d * d * 0.4, d), Math.copySign(e * e * 0.4, e), Math.copySign(g * g * 0.4, g));
        final SubLevel leashedSubLevel = helper.getContaining(leashedEntity);

        if (leashedSubLevel != null) {
            impulse = leashedSubLevel.logicalPose().transformNormalInverse(impulse);
        }

        original.call(leashedEntity, impulse);
    }
}
