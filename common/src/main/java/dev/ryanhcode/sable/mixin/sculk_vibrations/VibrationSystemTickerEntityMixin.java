package dev.ryanhcode.sable.mixin.sculk_vibrations;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.sculk_vibrations.GlobalPositionVibrationUser;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Entity counterpart of {@link VibrationSystemTickerMixin}
 */
@Mixin({Allay.class, Warden.class})
public class VibrationSystemTickerEntityMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$Ticker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$Data;Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$User;)V"))
    private void sable$useGlobalDestPos(final Level level, final VibrationSystem.Data data, final VibrationSystem.User user, final Operation<Void> original) {
        original.call(level, data, GlobalPositionVibrationUser.wrap(level, user));
    }
}
