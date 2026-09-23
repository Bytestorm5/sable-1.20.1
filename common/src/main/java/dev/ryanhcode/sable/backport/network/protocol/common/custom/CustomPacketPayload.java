package dev.ryanhcode.sable.backport.network.protocol.common.custom;

import net.minecraft.resources.ResourceLocation;

/**
 * A custom play packet, identified by its {@link Type}. Sent through {@link dev.ryanhcode.sable.network.tcp.SablePacketManager}.
 */
public interface CustomPacketPayload {

    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
