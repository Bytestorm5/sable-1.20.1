package dev.ryanhcode.sable.mixin.camera.camera_rotation;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix f3 crosshair when riding entity in sub-level
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewStack()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private void sable$onRenderCrosshair(final CallbackInfo ci, @Share("mountedOrientation") final LocalRef<Quaterniond> mountedOrientation) {
        final Camera camera = this.minecraft.gameRenderer.getMainCamera();
        final Entity entity = camera.getEntity();

        final float pt = this.minecraft.getFrameTime();
        final Quaterniond ridingOrientation = EntitySubLevelRotationHelper.getEntityOrientation(entity, (x) -> ((ClientSubLevel) x).renderPose(), pt, EntitySubLevelRotationHelper.Type.CAMERA);
        mountedOrientation.set(ridingOrientation);
    }

    /**
     * 1.20.1 rotates the model view {@link PoseStack} with {@code mulPose(Axis.XN.rotationDegrees(xRot))} (ordinal 0)
     * then {@code mulPose(Axis.YP.rotationDegrees(yRot))} (ordinal 1), where 1.21 used {@code Matrix4fStack#rotateX/Y}.
     */
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0))
    private void sable$redirectRotateX(final PoseStack stack, final Quaternionf rotation, @Share("mountedOrientation") final LocalRef<Quaterniond> mountedOrientation) {
        if (mountedOrientation.get() != null) {
            final float pt = this.minecraft.getFrameTime();
            final Camera camera = this.minecraft.gameRenderer.getMainCamera();
            final Entity entity = camera.getEntity();

            stack.mulPose(Axis.XN.rotationDegrees(entity.getViewXRot(pt)));
            return;
        }

        stack.mulPose(rotation);
    }

    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 1))
    private void sable$redirectRotateY(final PoseStack stack, final Quaternionf rotation, @Share("mountedOrientation") final LocalRef<Quaterniond> mountedOrientation) {
        if (mountedOrientation.get() != null) {
            final float pt = this.minecraft.getFrameTime();
            final Camera camera = this.minecraft.gameRenderer.getMainCamera();
            final Entity entity = camera.getEntity();

            stack.mulPose(Axis.YP.rotationDegrees(entity.getViewYRot(pt)));
            stack.mulPose(new Quaternionf(mountedOrientation.get()).conjugate());
            return;
        }

        stack.mulPose(rotation);
    }

}
