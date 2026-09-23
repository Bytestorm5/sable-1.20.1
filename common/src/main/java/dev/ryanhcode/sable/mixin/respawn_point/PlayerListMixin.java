package dev.ryanhcode.sable.mixin.respawn_point;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.ryanhcode.sable.mixinterface.respawn_point.ServerPlayerRespawnExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Respawning on sub-levels.
 * <p>
 * 1.21 resolves the respawn position in {@code ServerPlayer#findRespawnPositionAndUseSpawnBlock} and copies it with
 * {@code ServerPlayer#copyRespawnPosition}; on 1.20.1 both happen inline in {@link PlayerList#respawn}.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;findRespawnPositionAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3> sable$findRespawnPosition(final ServerLevel level,
                                                     final BlockPos blockPos,
                                                     final float angle,
                                                     final boolean forced,
                                                     final boolean keepEverything,
                                                     final Operation<Optional<Vec3>> original,
                                                     @Local(argsOnly = true) final ServerPlayer player,
                                                     @Share("subLevelRespawn") final LocalBooleanRef subLevelRespawn) {
        final Optional<Vec3> subLevelPosition = ((ServerPlayerRespawnExtension) player).sable$findSubLevelRespawnPosition(level);

        if (subLevelPosition != null) {
            subLevelRespawn.set(true);
            return subLevelPosition;
        }

        return original.call(level, blockPos, angle, forced, keepEverything);
    }

    /**
     * The respawn block position is in the plot, so don't let vanilla aim the player at it; use the respawn angle like 1.21
     */
    @WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;moveTo(DDDFF)V"))
    private void sable$respawnAngle(final ServerPlayer instance,
                                    final double x,
                                    final double y,
                                    final double z,
                                    final float yRot,
                                    final float xRot,
                                    final Operation<Void> original,
                                    @Local(argsOnly = true) final ServerPlayer player,
                                    @Share("subLevelRespawn") final LocalBooleanRef subLevelRespawn) {
        original.call(instance, x, y, z, subLevelRespawn.get() ? player.getRespawnAngle() : yRot, xRot);
    }

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V"))
    private void sable$copyRespawnPosition(final ServerPlayer instance,
                                           final ResourceKey<Level> dimension,
                                           final BlockPos pos,
                                           final float angle,
                                           final boolean forced,
                                           final boolean sendMessage,
                                           final Operation<Void> original,
                                           @Local(argsOnly = true) final ServerPlayer player) {
        if (((ServerPlayerRespawnExtension) player).sable$getRespawnPoint() != null) {
            ((ServerPlayerRespawnExtension) instance).sable$copyRespawnPosition(player);
            return;
        }

        original.call(instance, dimension, pos, angle, forced, sendMessage);
    }
}
