package dev.ryanhcode.sable.mixin.plot;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Only pairs entities inside plot chunks with players that are tracking the sub-level owning the plot.
 * <p>
 * On 1.21 this was achieved by hooking {@code ChunkMap#isChunkTracked}, which {@code TrackedEntity#updatePlayer} consulted.
 * 1.20.1 has no such check (entities are paired purely by distance), so we add the plot-chunk part of it here.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class TrackedEntityMixin {

    @Shadow
    @Final
    Entity entity;

    @ModifyExpressionValue(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean sable$isPlotChunkTracked(final boolean original, final ServerPlayer player) {
        if (!original) {
            return false;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(this.entity.level());

        if (container == null) {
            return true;
        }

        final LevelPlot plot = container.getPlot(this.entity.chunkPosition());

        if (plot != null) {
            final ServerSubLevel subLevel = (ServerSubLevel) plot.getSubLevel();
            return subLevel.getTrackingPlayers().contains(player.getGameProfile().getId());
        }

        return true;
    }
}
