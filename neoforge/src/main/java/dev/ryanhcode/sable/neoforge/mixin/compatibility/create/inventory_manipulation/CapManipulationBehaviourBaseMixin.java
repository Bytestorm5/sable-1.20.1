package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.inventory_manipulation;

import com.google.common.base.Predicate;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapManipulationBehaviourBase.class)
public class CapManipulationBehaviourBaseMixin {

    @Shadow(remap = false) protected Predicate<BlockEntity> filter;

    @Redirect(method = "findNewCapability", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    public BlockEntity sable$findNewCapOnSubLevel(final Level level, final BlockPos blockPos) {
        final ActiveSableCompanion helper = Sable.HELPER;
        return helper.runIncludingSubLevels(level, blockPos.getCenter(), true, helper.getContaining(level, blockPos), (subLevel, internalPos) -> {
            final BlockEntity caughtBE = level.getBlockEntity(internalPos);
            if (this.filter.apply(caughtBE)) {
                // On 1.20.1 the capability is then read from this block entity directly, so unlike on NeoForge no
                // position correction is needed afterwards
                return caughtBE;
            }

            return null;
        });
    }
}
