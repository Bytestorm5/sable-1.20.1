package dev.ryanhcode.sable.neoforge.network;

import dev.ryanhcode.sable.backport.network.codec.StreamCodec;
import dev.ryanhcode.sable.backport.network.protocol.common.custom.CustomPacketPayload;
import dev.ryanhcode.sable.network.tcp.SablePacketContext;
import dev.ryanhcode.sable.network.tcp.SablePacketManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * {@link SablePacketManager} on a Forge {@link SimpleChannel}. Every payload travels inside one of two wrapper messages
 * (one per direction) that carry the payload's type id followed by the payload itself.
 */
public class ForgeSablePacketManager implements SablePacketManager {

    /**
     * Every registered payload type, across all managers, so payloads can be turned into packets without a manager
     * reference.
     */
    static final Map<ResourceLocation, Registration<?>> REGISTRATIONS = new ConcurrentHashMap<>();

    private final SimpleChannel channel;

    public ForgeSablePacketManager(final String modId, final String version) {
        this.channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(modId, "main"),
                () -> version,
                // Like Veil's optional payload registrar: players can join servers without Sable and vice versa
                NetworkRegistry.acceptMissingOr(version),
                NetworkRegistry.acceptMissingOr(version));

        this.channel.messageBuilder(ClientboundMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buf) -> encode(message.payload(), buf))
                .decoder(buf -> new ClientboundMessage(decode(buf)))
                .consumerMainThread((message, context) -> handle(message.payload(), context))
                .add();
        this.channel.messageBuilder(ServerboundMessage.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> encode(message.payload(), buf))
                .decoder(buf -> new ServerboundMessage(decode(buf)))
                .consumerMainThread((message, context) -> handle(message.payload(), context))
                .add();
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(final CustomPacketPayload.Type<T> type, final StreamCodec<? super FriendlyByteBuf, T> codec, final PacketHandler<T> handler) {
        this.register(type, codec, handler, true);
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(final CustomPacketPayload.Type<T> type, final StreamCodec<? super FriendlyByteBuf, T> codec, final PacketHandler<T> handler) {
        this.register(type, codec, handler, false);
    }

    private <T extends CustomPacketPayload> void register(final CustomPacketPayload.Type<T> type, final StreamCodec<? super FriendlyByteBuf, T> codec, final PacketHandler<T> handler, final boolean clientbound) {
        final Registration<T> registration = new Registration<>(this, codec, handler, clientbound);
        if (REGISTRATIONS.putIfAbsent(type.id(), registration) != null) {
            throw new IllegalStateException("Duplicate packet registration: " + type.id());
        }
    }

    Packet<?> toPacket(final CustomPacketPayload payload) {
        final Registration<?> registration = registration(payload.type().id());
        if (registration.clientbound()) {
            return this.channel.toVanillaPacket(new ClientboundMessage(payload), NetworkDirection.PLAY_TO_CLIENT);
        }
        return this.channel.toVanillaPacket(new ServerboundMessage(payload), NetworkDirection.PLAY_TO_SERVER);
    }

    static Registration<?> registration(final ResourceLocation id) {
        final Registration<?> registration = REGISTRATIONS.get(id);
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered packet: " + id);
        }
        return registration;
    }

    @SuppressWarnings("unchecked")
    private static void encode(final CustomPacketPayload payload, final FriendlyByteBuf buf) {
        final ResourceLocation id = payload.type().id();
        final Registration<CustomPacketPayload> registration = (Registration<CustomPacketPayload>) registration(id);
        buf.writeResourceLocation(id);
        registration.codec().encode(buf, payload);
    }

    private static CustomPacketPayload decode(final FriendlyByteBuf buf) {
        final ResourceLocation id = buf.readResourceLocation();
        return registration(id).codec().decode(buf);
    }

    @SuppressWarnings("unchecked")
    private static void handle(final CustomPacketPayload payload, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        final Registration<CustomPacketPayload> registration = (Registration<CustomPacketPayload>) registration(payload.type().id());

        final SablePacketContext sableContext;
        if (context.getDirection().getReceptionSide().isClient()) {
            sableContext = ForgeSableClientPacketContext.create(registration.manager());
        } else {
            sableContext = new ServerContext(registration.manager(), context);
        }
        registration.handler().handlePacket(payload, sableContext);
    }

    record Registration<T extends CustomPacketPayload>(ForgeSablePacketManager manager,
                                                       StreamCodec<? super FriendlyByteBuf, T> codec,
                                                       PacketHandler<T> handler,
                                                       boolean clientbound) {
    }

    record ClientboundMessage(CustomPacketPayload payload) {
    }

    record ServerboundMessage(CustomPacketPayload payload) {
    }

    private record ServerContext(ForgeSablePacketManager manager, NetworkEvent.Context context) implements SablePacketContext {

        @Override
        public @Nullable net.minecraft.world.entity.player.Player player() {
            return this.context.getSender();
        }

        @Override
        public @Nullable net.minecraft.world.level.Level level() {
            return this.context.getSender() != null ? this.context.getSender().level() : null;
        }

        @Override
        public void sendPacket(final CustomPacketPayload payload) {
            this.context.getNetworkManager().send(this.manager.toPacket(payload));
        }

        @Override
        public void disconnect(final net.minecraft.network.chat.Component reason) {
            this.context.getNetworkManager().disconnect(reason);
        }
    }
}
