package dev.ryanhcode.sable.neoforge.mixin.block_entity_visible;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LevelRenderer.class, priority = 2000)
public class LevelRendererMixin {

    /**
     * NeoForge 1.21 checks block entity visibility through {@code ClientHooks#isBlockEntityRendererVisible}; Forge 1.20.1
     * checks {@code frustum.isVisible(blockEntity.getRenderBoundingBox())} inline, so the bounds are transformed instead.
     *
     * @author RyanH
     * @reason Take sub-levels into account for visibility check
     */
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getRenderBoundingBox()Lnet/minecraft/world/phys/AABB;", remap = false), require = 0)
    private AABB sable$getRenderBoundingBox(final BlockEntity blockEntity) {
        AABB renderBounds = blockEntity.getRenderBoundingBox();

        if (renderBounds.equals(IForgeBlockEntity.INFINITE_EXTENT_AABB)) {
            return renderBounds;
        }

        final SubLevel subLevel = Sable.HELPER.getContainingClient(renderBounds.getCenter());

        if (subLevel != null) {
            final BoundingBox3d bb = new BoundingBox3d(renderBounds);
            renderBounds = bb.transform(subLevel.logicalPose(), bb).toMojang();
        }

        return renderBounds;
    }
}
