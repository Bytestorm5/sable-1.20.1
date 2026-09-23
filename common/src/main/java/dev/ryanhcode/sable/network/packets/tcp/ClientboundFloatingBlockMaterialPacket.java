package dev.ryanhcode.sable.network.packets.tcp;

import foundry.veil.backport.network.codec.VanillaStreamCodecs;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import foundry.veil.api.network.handler.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import foundry.veil.backport.network.codec.StreamCodec;
import foundry.veil.backport.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundFloatingBlockMaterialPacket(ResourceLocation name, FloatingBlockMaterial material) implements SableTCPPacket {
    public static final Type<ClientboundFloatingBlockMaterialPacket> TYPE = new CustomPacketPayload.Type<>(Sable.sablePath("floating_material"));

    public static final StreamCodec<ByteBuf, ClientboundFloatingBlockMaterialPacket> CODEC = StreamCodec.composite(
            VanillaStreamCodecs.RESOURCE_LOCATION, ClientboundFloatingBlockMaterialPacket::name,
            FloatingBlockMaterial.STREAM_CODEC, ClientboundFloatingBlockMaterialPacket::material,
            ClientboundFloatingBlockMaterialPacket::new
    );

    @Override
    public void handle(PacketContext context) {
        Minecraft.getInstance().execute(() -> {
            FloatingBlockMaterialDataHandler.addMaterial(this.name, this.material);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
