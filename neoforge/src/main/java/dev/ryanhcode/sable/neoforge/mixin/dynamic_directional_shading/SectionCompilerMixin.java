package dev.ryanhcode.sable.neoforge.mixin.dynamic_directional_shading;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21's {@code SectionCompiler#compile} is {@code ChunkRenderDispatcher.RenderChunk.RebuildTask#compile} on 1.20.1.
 * The section position isn't a parameter there, so it is taken from the section origin local once the model block
 * renderer cache is enabled (which only happens when there is a region to compile). Forge 1.20.1 has no
 * {@code AddSectionGeometryEvent}, so the additional renderers parameter is gone.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class SectionCompilerMixin {

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;enableCaching()V", shift = At.Shift.AFTER))
    private void sable$preCompile(final float x, final float y, final float z, final ChunkBufferBuilderPack pack, final CallbackInfoReturnable<?> cir, @Local(ordinal = 0) final BlockPos origin) {
        final ClientLevel level = Minecraft.getInstance().level;
        final SubLevelContainer container = SubLevelContainer.getContainer(level);

        final LevelPlot plot = container.getPlot(SectionPos.of(origin).chunk());

        ((ModelBlockRendererCacheExtension) ModelBlockRenderer.CACHE.get()).sable$setOnSubLevel(plot != null);
    }

    @Inject(method = "compile", at = @At("TAIL"))
    private void sable$postCompile(final float x, final float y, final float z, final ChunkBufferBuilderPack pack, final CallbackInfoReturnable<?> cir) {
        ((ModelBlockRendererCacheExtension) ModelBlockRenderer.CACHE.get()).sable$setOnSubLevel(false);
    }

}
