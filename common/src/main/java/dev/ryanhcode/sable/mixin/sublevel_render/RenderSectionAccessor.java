package dev.ryanhcode.sable.mixin.sublevel_render;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;
import java.util.Set;

/**
 * Accessor for 1.20.1's {@link ChunkRenderDispatcher.RenderChunk} (1.21's {@code SectionRenderDispatcher.RenderSection}).
 */
@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public interface RenderSectionAccessor {

    @Accessor
    Set<BlockEntity> getGlobalBlockEntities();

    /**
     * {@code updateGlobalBlockEntities} is package-private on 1.20.1 (1.21 exposed it through an access transformer).
     */
    @Invoker("updateGlobalBlockEntities")
    void sable$updateGlobalBlockEntities(Collection<BlockEntity> blockEntities);
}
