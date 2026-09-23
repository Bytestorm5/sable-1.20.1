package dev.ryanhcode.sable.network.tcp;

import foundry.veil.api.network.handler.PacketContext;
import foundry.veil.backport.network.protocol.common.custom.CustomPacketPayload;

public interface SableTCPPacket extends CustomPacketPayload {

    void handle(PacketContext context);
}
