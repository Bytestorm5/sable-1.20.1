package dev.ryanhcode.sable.mixin.entity.entity_leashing;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * On 1.20.1 leash rendering lives in {@link MobRenderer#renderLeash} (1.21 moved it to {@code EntityRenderer}).
 * <p>
 * The leashed entity is typed as {@link Mob} there, so its {@code getEyePosition} call is owned by {@code Mob}, while the
 * leash holder's calls are owned by {@code Entity}. 1.21 redirected both eye position calls, so we do the same.
 */
@Mixin(MobRenderer.class)
public class EntityRendererMixin {

    @Redirect(method = "renderLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getRopeHoldPosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sable$getRopeHoldPosition(final Entity instance, final float f, @Local(argsOnly = true, ordinal = 0) final Mob leashedEntity){
        final ActiveSableCompanion helper = Sable.HELPER;
        final SubLevel leashedSubLevel = helper.getContaining(leashedEntity);

        final Vector3d ropeHoldPosition = JOMLConversion.toJOML(instance.getRopeHoldPosition(f));
        final SubLevel holdingSubLevel = helper.getContaining(leashedEntity.level(), ropeHoldPosition);

        if (holdingSubLevel != null) {
            holdingSubLevel.logicalPose().transformPosition(ropeHoldPosition);
        }

        if (leashedSubLevel != null) {
            leashedSubLevel.logicalPose().transformPositionInverse(ropeHoldPosition);
        }

        return JOMLConversion.toMojang(ropeHoldPosition);
    }

    @Redirect(method = "renderLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sable$getEyePosition(final Entity instance, final float f, @Local(argsOnly = true, ordinal = 0) final Mob leashedEntity){
        return sable$getAccountedEyePosition(instance, f, leashedEntity);
    }

    @Redirect(method = "renderLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sable$getLeashedEyePosition(final Mob instance, final float f, @Local(argsOnly = true, ordinal = 0) final Mob leashedEntity){
        return sable$getAccountedEyePosition(instance, f, leashedEntity);
    }

    @Unique
    private static Vec3 sable$getAccountedEyePosition(final Entity instance, final float f, final Mob leashedEntity) {
        final ActiveSableCompanion helper = Sable.HELPER;
        final SubLevel leashedSubLevel = helper.getContaining(leashedEntity);

        final Vector3d eyePosition = JOMLConversion.toJOML(instance.getEyePosition(f));
        final SubLevel holdingSubLevel = helper.getContaining(leashedEntity.level(), eyePosition);

        if (holdingSubLevel != null) {
            holdingSubLevel.logicalPose().transformPosition(eyePosition);
        }

        if (leashedSubLevel != null) {
            leashedSubLevel.logicalPose().transformPositionInverse(eyePosition);
        }

        return JOMLConversion.toMojang(eyePosition);
    }
}
