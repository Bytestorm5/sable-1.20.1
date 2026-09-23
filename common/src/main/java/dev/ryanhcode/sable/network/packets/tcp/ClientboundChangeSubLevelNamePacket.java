package dev.ryanhcode.sable.network.packets.tcp;

import foundry.veil.backport.network.codec.VanillaStreamCodecs;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import foundry.veil.backport.network.codec.ByteBufCodecs;
import foundry.veil.backport.network.codec.StreamCodec;
import foundry.veil.backport.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record ClientboundChangeSubLevelNamePacket(UUID subLevelID, @Nullable String name) implements SableTCPPacket {
    public static Type<ClientboundChangeSubLevelNamePacket> TYPE = new Type<>(Sable.sablePath("change_sub_level_name"));
    public static StreamCodec<FriendlyByteBuf, ClientboundChangeSubLevelNamePacket> CODEC = StreamCodec.composite(
            VanillaStreamCodecs.UUID,
            ClientboundChangeSubLevelNamePacket::subLevelID,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            (packet) -> Optional.ofNullable(packet.name()),
            (uuid, optionalName) -> new ClientboundChangeSubLevelNamePacket(uuid, optionalName.orElse(null)));

    public void handle(final PacketContext context) {
        final SubLevelContainer container = SubLevelContainer.getContainer(context.level());
        if (container != null) {
            final SubLevel subLevel = container.getSubLevel(this.subLevelID);

            if (subLevel != null) {
                subLevel.setName(this.name);
            } else {
                Sable.LOGGER.error("Attempted to set name for a client sub-level that does not exist!");
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}