package dev.ryanhcode.sable.neoforge.network;

import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import dev.ryanhcode.sable.network.tcp.SablePacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side packet context. Kept in its own class so dedicated servers never load client classes.
 */
final class ForgeSableClientPacketContext implements SablePacketContext {

    private final ForgeSablePacketManager manager;

    private ForgeSableClientPacketContext(final ForgeSablePacketManager manager) {
        this.manager = manager;
    }

    static SablePacketContext create(final ForgeSablePacketManager manager) {
        return new ForgeSableClientPacketContext(manager);
    }

    @Override
    public @Nullable Player player() {
        return Minecraft.getInstance().player;
    }

    @Override
    public @Nullable Level level() {
        return Minecraft.getInstance().level;
    }

    @Override
    public void sendPacket(final CustomPacketPayload payload) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(this.manager.toPacket(payload));
        }
    }

    @Override
    public void disconnect(final Component reason) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.getConnection().disconnect(reason);
        }
    }
}
