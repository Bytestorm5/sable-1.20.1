package dev.ryanhcode.sable.mixin.player_standup;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.CanFallAtleastHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21's {@code Player#canPlayerFitWithinBlocksAndEntitiesWhen} is {@link Entity#canEnterPose} on 1.20.1, which only
 * players call. We only change the behaviour for players, like 1.21 did.
 */
@Mixin(Entity.class)
public class EntityMixin {

    @WrapOperation(
            method = "canEnterPose",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"
            )
    )
    private boolean sable$noCollisionWithSubLevels(final Level instance, final Entity entity, final AABB aabb, final Operation<Boolean> original) {
        if (!original.call(instance, entity, aabb)) {
            return false;
        }

        if (!(entity instanceof Player)) {
            return true;
        }

        // If vanilla says no collision, also check sublevel blocks.
        // canFallAtleastWithSubLevels returns non-null when there IS a collision,
        // meaning the player does NOT fit → return false.
        return CanFallAtleastHelper.canFallAtleastWithSubLevels(instance, aabb) == null;
    }
}
