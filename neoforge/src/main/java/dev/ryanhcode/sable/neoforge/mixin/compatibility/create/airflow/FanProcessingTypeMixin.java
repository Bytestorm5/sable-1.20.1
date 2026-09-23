package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.airflow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes fan processing types found on sub-levels too.
 * <p>
 * The NeoForge version wraps {@link FanProcessingType#getAt} itself; Mixin 0.8.5 (Forge 1.20.1) doesn't support
 * injectors in interface mixins, so this wraps its only callers in {@link AirCurrent} instead.
 */
@Mixin(AirCurrent.class)
public class FanProcessingTypeMixin {

	@WrapOperation(method = {"rebuild", "findAffectedHandlers"}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;getAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;", remap = false), remap = false)
	private FanProcessingType sable$getAtIncludingSubLevels(final Level level, final BlockPos pos, final Operation<FanProcessingType> original) {
		final ActiveSableCompanion helper = Sable.HELPER;
		return helper.runIncludingSubLevels(level, pos.getCenter(), true, helper.getContaining(level, pos),
				(subLevel, relativePos) -> original.call(level, relativePos));
	}
}
