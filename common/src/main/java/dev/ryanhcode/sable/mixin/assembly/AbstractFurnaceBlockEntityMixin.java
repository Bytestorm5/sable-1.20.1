package dev.ryanhcode.sable.mixin.assembly;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow @Final private Object2IntOpenHashMap<ResourceLocation> recipesUsed;

    /**
     * On 1.20.1 the furnace declares {@code clearContent} itself (it isn't inherited from
     * {@code BaseContainerBlockEntity}), so we append to it instead of overriding it.
     */
    @Inject(method = "clearContent", at = @At("TAIL"))
    private void sable$clearRecipesUsed(final CallbackInfo ci) {
        this.recipesUsed.clear();
    }

}
