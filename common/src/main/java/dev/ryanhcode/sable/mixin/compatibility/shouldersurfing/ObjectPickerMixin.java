package dev.ryanhcode.sable.mixin.compatibility.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.world.phys.PickContext;
import com.github.exopandora.shouldersurfing.client.world.phys.ObjectPicker;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Shoulder Surfing 5.x moved the object picker to {@code client.world.phys}, and {@code pick} takes a {@link PickContext}
 * instead of the player.
 */
@Mixin(ObjectPicker.class)
public class ObjectPickerMixin {

    @Redirect(method = "pick", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D", remap = true))
    public double distanceTo(final Vec3 instance, final Vec3 vec, @Local(argsOnly = true) final PickContext context) {
        return Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(context.entity().level(), instance, vec));
    }

    @Redirect(method = "pickEntities", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", remap = true))
    public double distanceToSq(final Vec3 instance, final Vec3 vec, @Local(argsOnly = true) final PickContext context) {
        return Sable.HELPER.distanceSquaredWithSubLevels(context.entity().level(), instance, vec);
    }
}
