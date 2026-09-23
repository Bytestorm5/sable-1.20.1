package dev.ryanhcode.sable.neoforge.mixin.camera_rotation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.neoforge.mixinterface.camera_rotation.CameraRollExtension;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Forge 1.20.1 builds the view matrix from the camera yaw and pitch plus the {@link ViewportEvent.ComputeCameraAngles} roll,
 * instead of from the camera's rotation quaternion like 1.21. Adds the roll of the sub-level the camera entity is riding.
 *
 * @see CameraMixin
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/ViewportEvent$ComputeCameraAngles;getRoll()F", remap = false))
    private float sable$addSubLevelRoll(final ViewportEvent.ComputeCameraAngles event, final Operation<Float> original) {
        return original.call(event) + ((CameraRollExtension) event.getCamera()).sable$getSubLevelRoll();
    }
}
