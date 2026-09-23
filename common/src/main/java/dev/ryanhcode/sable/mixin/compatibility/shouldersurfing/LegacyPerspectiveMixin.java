package dev.ryanhcode.sable.mixin.compatibility.shouldersurfing;

import dev.ryanhcode.sable.mixinhelpers.compatibility.shouldersurfing.SablePerspectives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shoulder Surfing 5.x converts perspectives to its legacy {@code api.model.Perspective} for old plugins, switching over
 * the vanilla perspectives' ordinals. The sub-level perspectives have no legacy equivalent, so they are reported as
 * third person.
 */
@Mixin(value = com.github.exopandora.shouldersurfing.api.model.Perspective.class, remap = false)
public class LegacyPerspectiveMixin {

    @Inject(method = "fromNewApi", at = @At("HEAD"), cancellable = true)
    private static void sable$fromNewApi(final com.github.exopandora.shouldersurfing.api.client.Perspective perspective,
                                         final CallbackInfoReturnable<com.github.exopandora.shouldersurfing.api.model.Perspective> cir) {
        if (perspective == SablePerspectives.SUB_LEVEL_VIEW || perspective == SablePerspectives.SUB_LEVEL_VIEW_UNLOCKED) {
            cir.setReturnValue(com.github.exopandora.shouldersurfing.api.model.Perspective.THIRD_PERSON_BACK);
        }
    }
}
