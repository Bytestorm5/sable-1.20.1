package dev.ryanhcode.sable.mixin.sculk_vibrations;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.sculk_vibrations.GlobalPositionVibrationUser;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes vibration users on sub-levels use their global position when receiving vibrations.
 * <p>
 * 1.21 wrapped {@code getPositionSource()} inside {@code VibrationSystem.Ticker} itself. Forge 1.20.1's Mixin (0.8.5)
 * can't inject into interfaces, so the calls to {@code Ticker.tick} are wrapped instead, handing it a
 * {@link GlobalPositionVibrationUser}. The vibration positions themselves are already global, see
 * {@link VibrationSystemListenerMixin}.
 *
 * @see VibrationSystemTickerEntityMixin
 */
@Mixin({SculkSensorBlock.class, CalibratedSculkSensorBlock.class, SculkShriekerBlock.class})
public class VibrationSystemTickerMixin {

    /**
     * The ticks are in the (static) block entity ticker lambdas returned by {@code getTicker}
     */
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$Ticker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$Data;Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$User;)V"))
    private static void sable$useGlobalDestPos(final Level level, final VibrationSystem.Data data, final VibrationSystem.User user, final Operation<Void> original) {
        original.call(level, data, GlobalPositionVibrationUser.wrap(level, user));
    }
}
