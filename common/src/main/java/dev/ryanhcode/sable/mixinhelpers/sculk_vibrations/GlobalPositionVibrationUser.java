package dev.ryanhcode.sable.mixinhelpers.sculk_vibrations;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A {@link VibrationSystem.User} whose position source is projected out of the sub-level it is in.
 * <p>
 * On 1.21 Sable wrapped {@code getPositionSource()} inside {@code VibrationSystem.Ticker}'s static methods. Forge 1.20.1
 * ships Mixin 0.8.5, which can't inject into interfaces, so the user handed to {@code Ticker.tick} is wrapped instead,
 * which covers the same three calls ({@code trySelectAndScheduleVibration}, {@code tryReloadVibrationParticle} and
 * {@code receiveVibration}).
 */
public final class GlobalPositionVibrationUser implements VibrationSystem.User {

    private final VibrationSystem.User delegate;
    private final PositionSource globalSource;

    private GlobalPositionVibrationUser(final VibrationSystem.User delegate, final PositionSource globalSource) {
        this.delegate = delegate;
        this.globalSource = globalSource;
    }

    /**
     * @return a user with a global position source if the user is in a sub-level, otherwise the user itself
     */
    public static VibrationSystem.User wrap(final Level level, final VibrationSystem.User user) {
        if (!(level instanceof ServerLevel)) {
            return user;
        }

        final Optional<Vec3> position = user.getPositionSource().getPosition(level);
        if (position.isEmpty() || Sable.HELPER.getContaining(level, position.get()) == null) {
            return user;
        }

        final Vec3 globalPosition = Sable.HELPER.projectOutOfSubLevel(level, position.get());
        return new GlobalPositionVibrationUser(user, new BlockPositionSource(BlockPos.containing(globalPosition)));
    }

    @Override
    public int getListenerRadius() {
        return this.delegate.getListenerRadius();
    }

    @Override
    public PositionSource getPositionSource() {
        return this.globalSource;
    }

    @Override
    public boolean canReceiveVibration(final ServerLevel level, final BlockPos pos, final GameEvent gameEvent, final GameEvent.Context context) {
        return this.delegate.canReceiveVibration(level, pos, gameEvent, context);
    }

    @Override
    public void onReceiveVibration(final ServerLevel level, final BlockPos pos, final GameEvent gameEvent, @Nullable final Entity entity, @Nullable final Entity playerEntity, final float distance) {
        this.delegate.onReceiveVibration(level, pos, gameEvent, entity, playerEntity, distance);
    }

    @Override
    public TagKey<GameEvent> getListenableEvents() {
        return this.delegate.getListenableEvents();
    }

    @Override
    public boolean canTriggerAvoidVibration() {
        return this.delegate.canTriggerAvoidVibration();
    }

    @Override
    public boolean requiresAdjacentChunksToBeTicking() {
        return this.delegate.requiresAdjacentChunksToBeTicking();
    }

    @Override
    public int calculateTravelTimeInTicks(final float distance) {
        return this.delegate.calculateTravelTimeInTicks(distance);
    }

    @Override
    public boolean isValidVibration(final GameEvent gameEvent, final GameEvent.Context context) {
        return this.delegate.isValidVibration(gameEvent, context);
    }

    @Override
    public void onDataChanged() {
        this.delegate.onDataChanged();
    }
}
