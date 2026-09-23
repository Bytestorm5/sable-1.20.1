package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import dev.ryanhcode.sable.neoforge.network.ForgeSableNetworking;
import dev.ryanhcode.sable.network.tcp.SablePacketManager;
import dev.ryanhcode.sable.platform.SableNetworkPlatform;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public class SableNetworkPlatformImpl implements SableNetworkPlatform {

    @Override
    public SablePacketManager createPacketManager(final String modId, final String version) {
        return ForgeSableNetworking.createPacketManager(modId, version);
    }

    @Override
    public Packet<?> toPacket(final CustomPacketPayload payload) {
        return ForgeSableNetworking.toPacket(payload);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Packet<ClientGamePacketListener> toClientboundPacket(final CustomPacketPayload payload) {
        return (Packet<ClientGamePacketListener>) ForgeSableNetworking.toPacket(payload);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Packet<ServerGamePacketListener> toServerboundPacket(final CustomPacketPayload payload) {
        return (Packet<ServerGamePacketListener>) ForgeSableNetworking.toPacket(payload);
    }

    @Override
    public void sendToServer(final Packet<?> packet) {
        ForgeSableNetworking.sendToServer(packet);
    }
}
