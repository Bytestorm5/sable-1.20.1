package dev.ryanhcode.sable.platform;

import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import dev.ryanhcode.sable.network.tcp.SablePacketManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import org.jetbrains.annotations.ApiStatus;

/**
 * Loader networking for {@link SablePacketManager}.
 */
@ApiStatus.Internal
public interface SableNetworkPlatform {
    SableNetworkPlatform INSTANCE = SablePlatformUtil.load(SableNetworkPlatform.class);

    SablePacketManager createPacketManager(String modId, String version);

    /**
     * Wraps a registered payload in the vanilla packet for the direction it was registered in.
     */
    Packet<?> toPacket(CustomPacketPayload payload);

    Packet<ClientGamePacketListener> toClientboundPacket(CustomPacketPayload payload);

    Packet<ServerGamePacketListener> toServerboundPacket(CustomPacketPayload payload);

    /**
     * Sends a packet from the client to the server.
     */
    void sendToServer(Packet<?> packet);
}
