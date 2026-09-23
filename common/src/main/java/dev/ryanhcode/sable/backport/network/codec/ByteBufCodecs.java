package dev.ryanhcode.sable.backport.network.codec;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * Stream codecs for common types over a raw {@link ByteBuf}.
 */
public final class ByteBufCodecs {

    private static final int MAX_STRING_LENGTH = 32767;

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((buf, value) -> buf.writeByte(value), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((buf, value) -> buf.writeShort(value), ByteBuf::readShort);
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeVarInt(value),
            buf -> new FriendlyByteBuf(buf).readVarInt());
    public static final StreamCodec<ByteBuf, Long> LONG = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeVarLong(value),
            buf -> new FriendlyByteBuf(buf).readVarLong());
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(MAX_STRING_LENGTH);
    public static final StreamCodec<ByteBuf, UUID> UUID = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeUUID(value),
            buf -> new FriendlyByteBuf(buf).readUUID());
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeResourceLocation(value),
            buf -> new FriendlyByteBuf(buf).readResourceLocation());
    public static final StreamCodec<ByteBuf, Vector3f> VECTOR3F = StreamCodec.of(
            (buf, value) -> {
                buf.writeFloat(value.x());
                buf.writeFloat(value.y());
                buf.writeFloat(value.z());
            },
            buf -> new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()));

    private ByteBufCodecs() {
    }

    public static StreamCodec<ByteBuf, String> stringUtf8(final int maxLength) {
        return StreamCodec.of(
                (buf, value) -> new FriendlyByteBuf(buf).writeUtf(value, maxLength),
                buf -> new FriendlyByteBuf(buf).readUtf(maxLength));
    }

    /**
     * Encodes a value through its {@link Codec}, as NBT.
     */
    public static <T> StreamCodec<ByteBuf, T> fromCodec(final Codec<T> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    final Tag tag = codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, error -> {
                        throw new IllegalArgumentException("Failed to encode " + value + ": " + error);
                    });
                    new FriendlyByteBuf(buf).writeNbt(wrap(tag));
                },
                buf -> {
                    final net.minecraft.nbt.CompoundTag wrapper = new FriendlyByteBuf(buf).readNbt();
                    final Tag tag = wrapper == null ? null : wrapper.get("v");
                    return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, error -> {
                        throw new IllegalArgumentException("Failed to decode: " + error);
                    });
                });
    }

    // FriendlyByteBuf on 1.20.1 can only write compound tags, so wrap arbitrary tags.
    private static net.minecraft.nbt.CompoundTag wrap(final Tag tag) {
        final net.minecraft.nbt.CompoundTag wrapper = new net.minecraft.nbt.CompoundTag();
        wrapper.put("v", tag);
        return wrapper;
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<B, V> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.isPresent());
                    value.ifPresent(v -> codec.encode(buf, v));
                },
                buf -> buf.readBoolean() ? Optional.of(codec.decode(buf)) : Optional.empty());
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(final IntFunction<C> factory, final StreamCodec<? super B, V> codec) {
        return StreamCodec.of(
                (buf, value) -> {
                    new FriendlyByteBuf(buf).writeVarInt(value.size());
                    for (final V v : value) {
                        codec.encode(buf, v);
                    }
                },
                buf -> {
                    final int size = new FriendlyByteBuf(buf).readVarInt();
                    final C collection = factory.apply(size);
                    for (int i = 0; i < size; i++) {
                        collection.add(codec.decode(buf));
                    }
                    return collection;
                });
    }
}
