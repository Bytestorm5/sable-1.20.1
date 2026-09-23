package dev.ryanhcode.sable.neoforge.network;

import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import dev.ryanhcode.sable.network.tcp.SablePacketManager;
import net.minecraft.network.protocol.Packet;

public final class ForgeSableNetworking {

    private ForgeSableNetworking() {
    }

    public static SablePacketManager createPacketManager(final String modId, final String version) {
        return new ForgeSablePacketManager(modId, version);
    }

    public static Packet<?> toPacket(final CustomPacketPayload payload) {
        return ForgeSablePacketManager.registration(payload.type().id()).manager().toPacket(payload);
    }

    public static void sendToServer(final Packet<?> packet) {
        ClientAccess.sendToServer(packet);
    }

    // Separate class so the server never resolves client classes
    private static final class ClientAccess {
        private static void sendToServer(final Packet<?> packet) {
            final net.minecraft.client.multiplayer.ClientPacketListener connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(packet);
            }
        }
    }
}
