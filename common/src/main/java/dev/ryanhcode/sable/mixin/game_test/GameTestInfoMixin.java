package dev.ryanhcode.sable.mixin.game_test;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {

    @Shadow
    public abstract ServerLevel getLevel();

    @Shadow
    @Nullable
    public abstract Throwable getError();

    @Shadow
    public abstract AABB getStructureBounds();

    /**
     * 1.21 removes the entities in the structure bounds when a test succeeds, and we removed sub-levels alongside them.
     * 1.20.1 doesn't clean up entities there, so we remove sub-levels at the end of a successful {@code succeed}.
     */
    @Inject(method = "succeed", at = @At("TAIL"))
    public void removeSublevels(final CallbackInfo ci) {
        if (this.getError() != null) {
            return;
        }

        final AABB aabb = this.getStructureBounds();
        if (aabb == null) {
            return;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(this.getLevel());
        if (container != null) {
            for (final SubLevel subLevel : container.queryIntersecting(new BoundingBox3d(aabb))) {
                container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            }
        }
    }
}
