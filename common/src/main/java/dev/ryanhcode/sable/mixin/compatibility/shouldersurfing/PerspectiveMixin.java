package dev.ryanhcode.sable.mixin.compatibility.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.CrosshairVisibility;
import com.github.exopandora.shouldersurfing.api.client.Perspective;
import com.github.exopandora.shouldersurfing.api.config.IPerspectiveConfig;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.mixinhelpers.compatibility.shouldersurfing.SablePerspectives;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the sub-level camera types to Shoulder Surfing's perspectives.
 * <p>
 * Shoulder Surfing 5.x (the 1.20.1 build) moved {@code Perspective} to {@code api.client}, and its {@code next},
 * {@code isEnabled} and {@code of} switch over the vanilla perspectives' ordinals, which would throw for the added values.
 * So instead of adjusting the {@code next} local like on 4.x, the sub-level perspectives are handled before the switches run.
 */
@Mixin(value = Perspective.class, remap = false)
public class PerspectiveMixin {

    @Shadow
    @Final
    @Mutable
    private static Perspective[] $VALUES;

    static {
        final var subLevelView = create("SUB_LEVEL_VIEW", $VALUES.length, SableCameraTypes.SUB_LEVEL_VIEW, CrosshairVisibility.NEVER);

        $VALUES = ArrayUtils.add($VALUES, subLevelView);

        final var subLevelViewUnlocked = create("SUB_LEVEL_VIEW_UNLOCKED", $VALUES.length, SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED, CrosshairVisibility.NEVER);

        $VALUES = ArrayUtils.add($VALUES, subLevelViewUnlocked);
    }

    @SuppressWarnings("SameParameterValue")
    @Invoker(value = "<init>")
    private static Perspective create(final String name, final int ordinal, final CameraType cameraType, final CrosshairVisibility defaultCrosshairVisibility) {
        throw new AssertionError("Unreachable");
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "next", at = @At("HEAD"), cancellable = true)
    public void sable$next(final IPerspectiveConfig config, final CallbackInfoReturnable<Perspective> cir) {
        final Object self = this;

        if (self == SablePerspectives.SUB_LEVEL_VIEW) {
            cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED);
            return;
        }

        if (self == SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED) {
            cir.setReturnValue(sable$enabledOrNext(Perspective.THIRD_PERSON_FRONT, config));
            return;
        }

        if (config.isThirdPersonReplaced()) {
            if (self == Perspective.SHOULDER_SURFING) {
                cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW);
            }
        } else {
            // The normal logic would wrap around to our new values, but the next one should be first person
            if (self == Perspective.SHOULDER_SURFING) {
                cir.setReturnValue(sable$enabledOrNext(Perspective.FIRST_PERSON, config));
                return;
            }

            if (self == Perspective.THIRD_PERSON_BACK) {
                cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW);
            }
        }
    }

    /**
     * Mirrors the enabled check vanilla {@code next} does on its result
     */
    @Unique
    private static Perspective sable$enabledOrNext(final Perspective perspective, final IPerspectiveConfig config) {
        return perspective.isEnabled(config) ? perspective : perspective.next(config);
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true)
    public void isEnabled(final IPerspectiveConfig config, final CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == SablePerspectives.SUB_LEVEL_VIEW || (Object) this == SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void of(final CameraType cameraType, final boolean shoulderSurfing, final CallbackInfoReturnable<Perspective> cir) {
        if (cameraType == SableCameraTypes.SUB_LEVEL_VIEW) {
            cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW);
        }
        if (cameraType == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED) {
            cir.setReturnValue(SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED);
        }
    }
}
