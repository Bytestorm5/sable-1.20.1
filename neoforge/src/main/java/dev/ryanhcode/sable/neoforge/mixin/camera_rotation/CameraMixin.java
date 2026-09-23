package dev.ryanhcode.sable.neoforge.mixin.camera_rotation;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.neoforge.mixinterface.camera_rotation.CameraRollExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rotates the camera with the sub-level the camera entity is riding.
 * <p>
 * Differences from 1.21 NeoForge:
 * <ul>
 *     <li>1.20.1's {@code Camera#setRotation(float, float)} has no roll, and builds its basis vectors from the rotation
 *     right after {@code rotationYXZ}, so rotating the quaternion there is enough (no {@code FORWARDS/UP/LEFT} constants).</li>
 *     <li>1.20.1 uses {@code rotationYXZ(-yRot, xRot, 0)} with a +Z forward vector, so the yaw and pitch are recovered
 *     with the opposite signs to 1.21.</li>
 *     <li>The view matrix is built from yaw, pitch and the {@code ComputeCameraAngles} roll, so the sub-level roll is
 *     exposed through {@link CameraRollExtension} and added in {@link GameRendererMixin}.</li>
 *     <li>{@code ComputeCameraAngles} is fired by {@code GameRenderer} after {@code setup}, so the mirrored third person
 *     rotation uses the entity's view angles, which are what the 1.21 event carried by default.</li>
 * </ul>
 */
@Mixin(Camera.class)
public abstract class CameraMixin implements CameraRollExtension {

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Shadow
    private Entity entity;

    @Unique
    private float sable$subLevelRoll;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
    private void sable$redirectSetRotation(final Camera camera, final float f, final float g, @Local(argsOnly = true) final Entity entity, @Local(argsOnly = true) final float partialTick) {
        this.setRotation(entity.getViewYRot(partialTick) + 180.0f, -entity.getViewXRot(partialTick));
    }

    @WrapMethod(method = "setPosition(Lnet/minecraft/world/phys/Vec3;)V")
    private void sable$setPosition(final Vec3 arg, final Operation<Void> original) {
        if (this.entity == null) {
            original.call(arg);
            return;
        }
        final Level level = this.entity.level();

        final ClientSubLevel subLevel = (ClientSubLevel) Sable.HELPER.getContaining(level, arg);
        if(subLevel == null) {
            original.call(arg);
            return;
        }

        final Pose3dc pose = subLevel.renderPose();
        final Vec3 pos = pose.transformPosition(arg);
        original.call(pos);
    }

    @Inject(method = "setRotation(FF)V", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", shift = At.Shift.AFTER, remap = false))
    public void sable$rotateView(final float f, final float g, final CallbackInfo ci) {
        this.sable$subLevelRoll = 0.0f;

        final float pt = Minecraft.getInstance().getFrameTime();
        final Quaterniond ridingOrientation = EntitySubLevelRotationHelper.getEntityOrientation(this.entity, (x) -> ((ClientSubLevel) x).renderPose(), pt, EntitySubLevelRotationHelper.Type.CAMERA);

        if (ridingOrientation != null) {
            // The forwards / up / left vectors are rotated by vanilla right after this
            this.rotation.premul(new Quaternionf(ridingOrientation));

            final Vector3f euler = this.rotation.getEulerAnglesYXZ(new Vector3f());
            this.yRot = (float) -Math.toDegrees(euler.y);
            this.xRot = (float) Math.toDegrees(euler.x);
            this.sable$subLevelRoll = (float) Math.toDegrees(euler.z);
        }
    }

    @Override
    public float sable$getSubLevelRoll() {
        return this.sable$subLevelRoll;
    }
}
