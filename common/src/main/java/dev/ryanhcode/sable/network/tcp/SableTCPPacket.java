package dev.ryanhcode.sable.network.tcp;

import dev.ryanhcode.sable.network.tcp.SablePacketContext;
import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;

public interface SableTCPPacket extends CustomPacketPayload {

    void handle(SablePacketContext context);
}
