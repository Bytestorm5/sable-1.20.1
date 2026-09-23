package dev.ryanhcode.sable.network.tcp;

import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Context for a received Sable packet. Handlers are always called on the main thread of the receiving side.
 */
public interface SablePacketContext {

    /**
     * @return the sending player on the server, or the local player on the client
     */
    @Nullable
    Player player();

    /**
     * @return the level of the sending player on the server, or the client level on the client
     */
    @Nullable
    Level level();

    /**
     * Sends a packet back to the other side.
     */
    void sendPacket(CustomPacketPayload payload);

    void disconnect(Component reason);
}
