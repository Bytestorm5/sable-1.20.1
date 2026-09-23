package dev.ryanhcode.sable.network.tcp;

import dev.ryanhcode.sable.backport.network.codec.StreamCodec;
import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import dev.ryanhcode.sable.platform.SableNetworkPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers and sends Sable's custom play packets. On 1.21 this was Veil's {@code VeilPacketManager}; on 1.20.1 Sable
 * carries its own implementation on top of the loader's networking, with the same shape.
 */
public interface SablePacketManager {

    static SablePacketManager create(final String modId, final String version) {
        return SableNetworkPlatform.INSTANCE.createPacketManager(modId, version);
    }

    /**
     * Registers a packet sent from the server to the client.
     */
    <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, PacketHandler<T> handler);

    /**
     * Registers a packet sent from the client to the server.
     */
    <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, PacketHandler<T> handler);

    /**
     * Wraps a registered clientbound payload into a vanilla packet, e.g. to bundle it with other packets.
     */
    static Packet<ClientGamePacketListener> toClientbound(final CustomPacketPayload payload) {
        return SableNetworkPlatform.INSTANCE.toClientboundPacket(payload);
    }

    /**
     * Wraps a registered serverbound payload into a vanilla packet.
     */
    static Packet<ServerGamePacketListener> toServerbound(final CustomPacketPayload payload) {
        return SableNetworkPlatform.INSTANCE.toServerboundPacket(payload);
    }

    /**
     * @return a sink sending to the server. Only valid on the client.
     */
    static PacketSink server() {
        return SableNetworkPlatform.INSTANCE::sendToServer;
    }

    static PacketSink player(final ServerPlayer player) {
        return packet -> player.connection.send(packet);
    }

    static PacketSink level(final ServerLevel level) {
        return packet -> level.getServer().getPlayerList().broadcastAll(packet, level.dimension());
    }

    static PacketSink all(final MinecraftServer server) {
        return packet -> server.getPlayerList().broadcastAll(packet);
    }

    /**
     * Somewhere packets can be sent.
     */
    @FunctionalInterface
    interface PacketSink {

        default void sendPacket(final CustomPacketPayload... payloads) {
            for (final CustomPacketPayload payload : payloads) {
                this.sendPacket(SableNetworkPlatform.INSTANCE.toPacket(payload));
            }
        }

        void sendPacket(Packet<?> packet);
    }

    @FunctionalInterface
    interface PacketHandler<T extends CustomPacketPayload> {
        void handlePacket(T payload, SablePacketContext context);
    }
}
