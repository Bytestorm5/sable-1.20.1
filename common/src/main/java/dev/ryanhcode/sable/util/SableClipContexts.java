package dev.ryanhcode.sable.util;

import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 1.20.1's {@link ClipContext} can only be built from an (optional, on Forge) entity; 1.20.5+ added a
 * {@link CollisionContext} constructor.
 */
public final class SableClipContexts {

    private SableClipContexts() {
    }

    public static ClipContext create(final Vec3 from, final Vec3 to, final ClipContext.Block block, final ClipContext.Fluid fluid, final CollisionContext collisionContext) {
        final ClipContext context = new ClipContext(from, to, block, fluid, (Entity) null);
        ((ClipContextExtension) context).sable$setCollisionContext(collisionContext);
        return context;
    }
}
